package com.prepcreatine.service;

import com.prepcreatine.domain.User;
import com.prepcreatine.dto.request.*;
import com.prepcreatine.dto.response.UserResponse;
import com.prepcreatine.exception.*;
import com.prepcreatine.repository.*;
import com.prepcreatine.util.DateUtil;
import com.prepcreatine.util.ReadinessScoreUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * User profile management per BSDD §12.
 * Handles: GET /api/me, PATCH /api/me, GET /api/profile/{shareToken}
 * Also computes and persists readiness score.
 */
@Service
@Transactional
public class UserService {

    private final UserRepository         userRepo;
    private final TestSessionRepository  testSessionRepo;
    private final UserTopicProgressRepository progressRepo;
    private final UserMapper             mapper;

    public UserService(UserRepository userRepo,
                       TestSessionRepository testSessionRepo,
                       UserTopicProgressRepository progressRepo,
                       UserMapper mapper) {
        this.userRepo        = userRepo;
        this.testSessionRepo = testSessionRepo;
        this.progressRepo    = progressRepo;
        this.mapper          = mapper;
    }

    @Transactional(readOnly = true)
    public UserResponse getMe(UUID userId) {
        User user = findActiveUser(userId);
        return mapper.toResponse(user);
    }

    public UserResponse updateProfile(UUID userId, UpdateProfileRequest req) {
        User user = findActiveUser(userId);
        if (req.fullName() != null && !req.fullName().isBlank()) {
            user.setFullName(req.fullName().trim());
        }
        if (req.examType() != null)     user.setExamType(req.examType());
        if (req.examDate() != null)     user.setExamDate(req.examDate());
        if (req.studyMode() != null)    user.setStudyMode(req.studyMode());
        if (req.dailyGoalMins() != null) user.setDailyGoalMins(req.dailyGoalMins());

        recomputeReadinessScore(user);
        return mapper.toResponse(userRepo.save(user));
    }

    public void softDeleteAccount(UUID userId) {
        User user = findActiveUser(userId);
        user.setActive(false);
        user.setEmail("deleted+" + userId + "@prepcreatine.invalid");
        userRepo.save(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getPublicProfile(String shareToken) {
        User user = userRepo.findByShareToken(shareToken)
            .orElseThrow(() -> new ResourceNotFoundException("Profile not found."));
        if (!user.isActive()) throw new ResourceNotFoundException("Profile not found.");
        return mapper.toResponse(user);
    }

    /**
     * Recomputes readiness score and persists it on the user.
     * Formula per BSDD §12:
     *   completionPct * 0.50 + avgTestScore * 0.35 + daysRemainingFactor * 0.15
     */
    public void recomputeReadinessScore(User user) {
        // 1. Completion percentage from topic progress
        long total    = progressRepo.countByUserId(user.getId());
        long mastered = progressRepo.countByUserIdAndStatus(user.getId(), "mastered");
        double completionPct = total == 0 ? 0.0 : (mastered * 100.0 / total);

        // 2. Average test score (last 10 completed tests)
        Double avgScore = testSessionRepo.findAvgScoreForUser(user.getId());
        double avgTestScore = avgScore != null ? avgScore : 0.0;

        // 3. Days remaining factor
        long daysLeft = DateUtil.daysUntilExam(user.getExamDate());
        double factor = DateUtil.daysRemainingFactor(daysLeft);

        user.setReadinessScore(ReadinessScoreUtil.compute(completionPct, avgTestScore, factor));
    }

    private User findActiveUser(UUID userId) {
        return userRepo.findByIdAndIsActiveTrue(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }
}

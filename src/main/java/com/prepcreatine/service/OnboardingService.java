package com.prepcreatine.service;

import com.prepcreatine.domain.*;
import com.prepcreatine.dto.request.*;
import com.prepcreatine.dto.response.UserResponse;
import com.prepcreatine.exception.*;
import com.prepcreatine.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Onboarding service per BSDD §9.
 * Three paths:
 *   Path A: exam_prep  — full profile (examType, examDate, studyMode, ...)
 *   Path B: topic_deep_dive — guided topic, optional exam
 *   Path C: speed_run  — custom topic, no exam date required
 *
 * On completion, triggers AI to generate initial context via ContextService.
 */
@Service
@Transactional
public class OnboardingService {

    private static final Logger log = LoggerFactory.getLogger(OnboardingService.class);

    private final UserRepository             userRepo;
    private final MentorStudentLinkRepository mentorLinkRepo;
    private final UserContextRepository      contextRepo;
    private final UserMapper                 mapper;
    private final EmailService               emailService;

    public OnboardingService(UserRepository userRepo,
                             MentorStudentLinkRepository mentorLinkRepo,
                             UserContextRepository contextRepo,
                             UserMapper mapper,
                             EmailService emailService) {
        this.userRepo        = userRepo;
        this.mentorLinkRepo  = mentorLinkRepo;
        this.contextRepo     = contextRepo;
        this.mapper          = mapper;
        this.emailService    = emailService;
    }

    public UserResponse complete(UUID userId, OnboardingCompleteRequest req) {
        User user = findUser(userId);

        if (user.isOnboardingComplete()) {
            throw new ValidationException("Onboarding is already complete.");
        }

        user.setExamType(req.examType());
        user.setExamDate(req.examDate());
        user.setStudyMode(req.studyMode());
        user.setDailyGoalMins(req.dailyGoalMins() != null ? req.dailyGoalMins() : 60);
        user.setOnboardingComplete(true);

        // Optional mentor code linkage
        if (req.mentorCode() != null && !req.mentorCode().isBlank()) {
            linkMentor(user, req.mentorCode().trim().toUpperCase());
        }

        userRepo.save(user);
        log.info("[Onboarding] Path A complete for userId={}", userId);
        return mapper.toResponse(user);
    }

    public UserResponse completeQuick(UUID userId, OnboardingQuickRequest req) {
        User user = findUser(userId);

        if (user.isOnboardingComplete()) {
            throw new ValidationException("Onboarding is already complete.");
        }

        user.setStudyMode("speed_run");
        user.setDailyGoalMins(30);
        user.setOnboardingComplete(true);
        userRepo.save(user);

        // Seed user context for the custom topic
        UserContext context = new UserContext();
        context.setUserId(userId);
        context.setCustomTopic(req.customTopic());
        contextRepo.save(context);

        log.info("[Onboarding] Path C (speed_run) complete for userId={}", userId);
        return mapper.toResponse(user);
    }

    // ── Private ─────────────────────────────────────────────────────────────

    private void linkMentor(User student, String mentorCode) {
        userRepo.findByMentorCodeAndIsActiveTrue(mentorCode).ifPresentOrElse(mentor -> {
            MentorStudentLink link = new MentorStudentLink();
            link.setMentorId(mentor.getId());
            link.setStudentId(student.getId());
            mentorLinkRepo.save(link);

            emailService.sendMentorConnectedEmail(student.getEmail(), student.getFullName(), mentor.getFullName());
            log.info("[Onboarding] Student {} linked to mentor {}", student.getId(), mentor.getId());
        }, () -> {
            throw new ValidationException("Mentor code not found: " + mentorCode);
        });
    }

    private User findUser(UUID userId) {
        return userRepo.findByIdAndIsActiveTrue(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }
}

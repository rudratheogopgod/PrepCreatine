package com.prepcreatine.service;

import com.prepcreatine.domain.*;
import com.prepcreatine.dto.response.*;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Maps JPA domain entities to response DTOs.
 * [DRY] Centralised mapping avoids duplication across services.
 */
@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getRole(),
            user.isOnboardingComplete(),
            user.isEmailVerified(),
            user.getExamType(),
            user.getExamDate(),
            user.getStudyMode(),
            user.getDailyGoalMins(),
            user.getCurrentStreak(),
            user.getLongestStreak(),
            user.getTotalDays(),
            user.getReadinessScore(),
            user.getShareToken(),
            user.getCreatedAt()
        );
    }

    public UserSummaryResponse toSummary(User user) {
        return new UserSummaryResponse(
            user.getId(),
            user.getFullName(),
            user.getRole(),
            user.getCurrentStreak(),
            user.getReadinessScore()
        );
    }
}

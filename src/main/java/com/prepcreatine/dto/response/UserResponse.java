package com.prepcreatine.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Returned by /api/auth/signup, /api/auth/login, and /api/me. */
public record UserResponse(
    UUID id,
    String email,
    String fullName,
    String role,
    boolean onboardingComplete,
    boolean emailVerified,
    String examType,
    LocalDate examDate,
    String studyMode,
    Integer dailyGoalMins,
    int currentStreak,
    int longestStreak,
    int totalDays,
    int readinessScore,
    String shareToken,
    OffsetDateTime createdAt
) {}

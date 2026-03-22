package com.prepcreatine.dto.request;

import java.time.LocalDate;

/** PATCH /api/me — all fields optional. */
public record UpdateProfileRequest(
    String fullName,
    String examType,
    LocalDate examDate,
    String studyMode,
    Integer dailyGoalMins
) {}

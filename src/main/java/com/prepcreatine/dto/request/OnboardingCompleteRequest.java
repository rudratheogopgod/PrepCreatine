package com.prepcreatine.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record OnboardingCompleteRequest(
    @NotBlank String examType,
    @NotNull  LocalDate examDate,
    @NotBlank String studyMode,
    Integer dailyGoalMins,
    String mentorCode
) {}

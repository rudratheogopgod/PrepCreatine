package com.prepcreatine.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Path C (speed_run) — just a topic, no exam type or date. */
public record OnboardingQuickRequest(
    @NotBlank String customTopic
) {}

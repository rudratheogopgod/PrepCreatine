package com.prepcreatine.dto.response;

import java.util.UUID;

/** Short form — used in paginated lists (community threads, mentor student list). */
public record UserSummaryResponse(
    UUID id,
    String fullName,
    String role,
    int currentStreak,
    int readinessScore
) {}

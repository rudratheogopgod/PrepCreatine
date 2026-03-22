package com.prepcreatine.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    String type,
    String title,
    String body,
    boolean isRead,
    String actionUrl,
    OffsetDateTime createdAt
) {}

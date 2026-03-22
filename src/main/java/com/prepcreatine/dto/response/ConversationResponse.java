package com.prepcreatine.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConversationResponse(
    UUID id,
    String title,
    UUID sourceId,
    String sourceName,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}

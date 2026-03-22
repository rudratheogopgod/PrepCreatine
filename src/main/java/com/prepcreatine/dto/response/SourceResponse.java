package com.prepcreatine.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SourceResponse(
    UUID id,
    String title,
    String sourceType,
    String url,
    String status,
    int chunkCount,
    Object studyGuide,  // JSONB — null until ready
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}

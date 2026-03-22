package com.prepcreatine.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Full message with parsed AI fields (concept map, youtube ids). */
public record MessageResponse(
    UUID id,
    UUID conversationId,
    String role,            // 'user' or 'assistant'
    String content,
    Object conceptMap,      // JSONB parsed — null for user messages
    List<String> youtubeIds,
    List<String> sourceChunkIds,
    OffsetDateTime createdAt
) {}

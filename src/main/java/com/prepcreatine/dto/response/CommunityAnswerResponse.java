package com.prepcreatine.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CommunityAnswerResponse(
    UUID id,
    UUID threadId,
    UserSummaryResponse author,
    String body,
    int upvoteCount,
    boolean isAccepted,
    boolean isMentorAnswer,
    boolean upvotedByMe,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}

package com.prepcreatine.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CommunityThreadResponse(
    UUID id,
    UserSummaryResponse author,
    String examId,
    String subjectId,
    String topicId,
    String title,
    String body,
    int upvoteCount,
    int answerCount,
    boolean isResolved,
    boolean upvotedByMe,
    String aiSummary,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}

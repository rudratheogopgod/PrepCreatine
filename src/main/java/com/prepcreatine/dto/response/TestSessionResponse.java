package com.prepcreatine.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TestSessionResponse(
    UUID id,
    String examId,
    String subjectId,
    String topicId,
    String status,
    int totalQuestions,
    Integer answeredCount,
    Integer correctCount,
    BigDecimal score,
    int timeLimitMins,
    OffsetDateTime createdAt,
    OffsetDateTime submittedAt,
    List<QuestionResponse> questions  // null when listing; populated when fetching detail
) {}

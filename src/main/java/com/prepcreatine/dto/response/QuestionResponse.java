package com.prepcreatine.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Question as returned to client.
 * correctAnswer and explanation only included after test is submitted.
 */
public record QuestionResponse(
    UUID id,
    String topicId,
    String subjectId,
    String examId,
    String type,
    Short level,
    String stem,
    List<String> options,         // 4 options for MCQ
    String correctAnswer,         // null until after submission
    String explanation,           // null until after submission
    String userAnswer,            // student's submitted answer
    Boolean isCorrect,            // null until after submission
    int timeTakenSecs
) {}

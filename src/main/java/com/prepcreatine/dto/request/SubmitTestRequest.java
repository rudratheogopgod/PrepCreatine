package com.prepcreatine.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Submit all answers at once for a started test session. */
public record SubmitTestRequest(
    @NotNull UUID testSessionId,
    @NotNull java.util.List<AnswerItem> answers
) {
    public record AnswerItem(
        @NotNull UUID questionId,
        String answer,        // null = unattempted
        int timeTakenSecs,
        boolean markedReview
    ) {}
}

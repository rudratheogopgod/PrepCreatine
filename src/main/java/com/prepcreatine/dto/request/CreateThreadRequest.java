package com.prepcreatine.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateThreadRequest(
    @NotBlank String examId,
    String subjectId,
    String topicId,
    @NotBlank @Size(max = 300) String title,
    @Size(max = 5000) String body
) {}

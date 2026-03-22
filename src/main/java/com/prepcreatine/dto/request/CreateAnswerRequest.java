package com.prepcreatine.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAnswerRequest(
    @NotBlank @Size(max = 5000) String body
) {}

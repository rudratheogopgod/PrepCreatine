package com.prepcreatine.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ImportTextRequest(
    @NotBlank String text,
    @NotBlank String title
) {}

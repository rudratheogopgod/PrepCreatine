package com.prepcreatine.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ImportUrlRequest(
    @NotBlank String url,
    String title // optional override
) {}

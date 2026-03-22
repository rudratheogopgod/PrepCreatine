package com.prepcreatine.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
    @NotBlank String currentPassword,
    @NotBlank @Size(min = 8, max = 72, message = "New password must be 8-72 characters")
    String newPassword,
    @NotBlank String confirmPassword
) {}

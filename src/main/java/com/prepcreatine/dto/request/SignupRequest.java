package com.prepcreatine.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
    @NotBlank @Email(message = "Invalid email address")
    String email,

    @NotBlank @Size(min = 8, max = 72, message = "Password must be 8-72 characters")
    String password,

    @NotBlank @Size(min = 2, max = 100, message = "Full name must be 2-100 characters")
    String fullName
) {}

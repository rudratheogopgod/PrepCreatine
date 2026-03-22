package com.prepcreatine.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ChatMessageRequest(
    @NotBlank @Size(max = 4000, message = "Message cannot exceed 4000 characters")
    String message,
    UUID conversationId  // null = create new conversation
) {}

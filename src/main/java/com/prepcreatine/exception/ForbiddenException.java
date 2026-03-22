package com.prepcreatine.exception;

import java.util.UUID;

public class ForbiddenException extends RuntimeException {
    private final String userId;

    public ForbiddenException(String message) {
        super(message);
        this.userId = "unknown";
    }

    public ForbiddenException(String message, UUID userId) {
        super(message);
        this.userId = userId != null ? userId.toString() : "unknown";
    }

    public String getUserId() { return userId; }
}

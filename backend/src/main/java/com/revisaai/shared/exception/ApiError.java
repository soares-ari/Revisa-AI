package com.revisaai.shared.exception;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        int status,
        String message,
        Instant timestamp,
        Map<String, String> errors
) {
    public ApiError(int status, String message) {
        this(status, message, Instant.now(), Map.of());
    }

    public ApiError(int status, String message, Map<String, String> errors) {
        this(status, message, Instant.now(), errors);
    }
}

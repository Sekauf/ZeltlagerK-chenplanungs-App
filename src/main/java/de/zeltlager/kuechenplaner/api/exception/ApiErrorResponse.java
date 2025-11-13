package de.zeltlager.kuechenplaner.api.exception;

import java.time.Instant;

/**
 * Standardized structure for error responses returned by the API.
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String code,
        String path) {
}

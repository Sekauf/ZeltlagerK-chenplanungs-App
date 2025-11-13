package de.zeltlager.kuechenplaner.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Indicates that the request cannot be processed because of a conflict with the
 * current state of the resource.
 */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "conflict", message);
    }

    public ConflictException(String message, Throwable cause) {
        super(HttpStatus.CONFLICT, "conflict", message, cause);
    }
}

package de.zeltlager.kuechenplaner.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Signals that the client sent a malformed or invalid request.
 */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "bad_request", message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, "bad_request", message, cause);
    }
}

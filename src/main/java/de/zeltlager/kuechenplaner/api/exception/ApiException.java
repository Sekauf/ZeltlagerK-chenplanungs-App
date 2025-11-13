package de.zeltlager.kuechenplaner.api.exception;

import java.util.Objects;

import org.springframework.http.HttpStatus;

/**
 * Base class for all API related exceptions that should be translated into
 * HTTP responses.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    protected ApiException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = Objects.requireNonNull(status, "status");
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
    }

    protected ApiException(HttpStatus status, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.status = Objects.requireNonNull(status, "status");
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

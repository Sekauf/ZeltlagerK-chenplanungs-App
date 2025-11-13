package de.zeltlager.kuechenplaner.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Indicates that authentication is required to access the resource.
 */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "unauthorized", message);
    }
}

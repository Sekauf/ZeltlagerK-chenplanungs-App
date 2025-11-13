package de.zeltlager.kuechenplaner.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Indicates that the user is authenticated but does not have sufficient
 * permissions.
 */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, "forbidden", message);
    }
}

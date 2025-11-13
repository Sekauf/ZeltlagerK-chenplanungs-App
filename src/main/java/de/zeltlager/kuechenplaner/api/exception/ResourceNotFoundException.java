package de.zeltlager.kuechenplaner.api.exception;

import java.util.Locale;

import org.springframework.http.HttpStatus;

/**
 * Indicates that a requested resource could not be located.
 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "resource_not_found", message);
    }

    public ResourceNotFoundException(String resourceName, Object identifier) {
        this(String.format(Locale.GERMAN, "%s mit Kennung %s wurde nicht gefunden", resourceName, identifier));
    }
}

package com.bunsen.api.aftercare.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends ApiException {

    private static final String REASON = "DUPLICATE_RESOURCE";

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(HttpStatus.CONFLICT, REASON,
                String.format("%s already exists with %s: '%s'. Please use a unique value.", resourceName, fieldName, fieldValue));
    }

    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, REASON, message);
    }
}
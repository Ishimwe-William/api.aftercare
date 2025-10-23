package com.bunsen.api.aftercare.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {

    private static final String REASON = "RESOURCE_NOT_FOUND";

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, REASON, message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(HttpStatus.NOT_FOUND, REASON,
                String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
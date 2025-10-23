package com.bunsen.api.aftercare.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends ApiException {

    private static final String REASON = "VALIDATION_FAILED";

    public ValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, REASON, "Validation failed: " + message);
    }
}
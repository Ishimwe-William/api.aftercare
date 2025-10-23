package com.bunsen.api.aftercare.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {

    private static final String REASON = "BAD_REQUEST_ERROR";

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, REASON, message);
    }
}
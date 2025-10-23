package com.bunsen.api.aftercare.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends ApiException {

    private static final String REASON = "UNAUTHORIZED_ACCESS";

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, REASON, "Access denied. " + message);
    }
}
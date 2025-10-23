package com.bunsen.api.aftercare.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base class for all custom API exceptions.
 * Provides a standardized way to convey HTTP status and error messages.
 */
@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorReason;

    public ApiException(HttpStatus status, String errorReason, String message) {
        super(message);
        this.status = status;
        this.errorReason = errorReason;
    }

    public ApiException(HttpStatus status, String errorReason, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorReason = errorReason;
    }

}
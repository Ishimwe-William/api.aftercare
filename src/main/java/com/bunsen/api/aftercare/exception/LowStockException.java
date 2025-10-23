package com.bunsen.api.aftercare.exception;

import org.springframework.http.HttpStatus;

public class LowStockException extends ApiException {

    private static final String REASON = "INSUFFICIENT_STOCK";

    public LowStockException(String message) {
        super(HttpStatus.BAD_REQUEST, REASON, message);
    }

    public LowStockException(String partName, int available, int required) {
        super(HttpStatus.BAD_REQUEST, REASON,
                String.format("Low stock for part '%s'. Available: %d, Required: %d.", partName, available, required));
    }
}
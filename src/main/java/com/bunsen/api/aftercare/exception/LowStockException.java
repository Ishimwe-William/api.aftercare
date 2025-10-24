package com.bunsen.api.aftercare.exception;

import org.springframework.http.HttpStatus;

public class LowStockException extends ApiException {

    private static final String REASON = "INSUFFICIENT_STOCK";

    public LowStockException(String message) {
        super(HttpStatus.BAD_REQUEST, REASON, message);
    }

    public LowStockException(String partName, Double available, Double required) {
        super(HttpStatus.BAD_REQUEST, REASON,
                String.format("Low stock for part '%s'. Available: %.2f, Required: %.2f.", partName, available, required));
    }

}
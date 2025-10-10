package com.bunsen.api.aftercare.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class LowStockException extends RuntimeException {
    public LowStockException(String message) {
        super(message);
    }

    public LowStockException(String partName, int available, int required) {
        super(String.format("Low stock for part '%s': available %d, required %d", partName, available, required));
    }
}

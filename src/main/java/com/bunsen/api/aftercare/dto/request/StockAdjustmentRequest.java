package com.bunsen.api.aftercare.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class StockAdjustmentRequest {
    @NotNull(message = "Target quantity is required")
    @DecimalMin(value = "0.0", message = "Target quantity must be zero or greater")
    private Double targetQuantity;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
package com.bunsen.api.aftercare.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class StockAdjustmentRequest {
    @NotBlank(message = "Adjustment type is required (ADD or SUBTRACT)")
    @Pattern(regexp = "ADD|SUBTRACT", message = "Adjustment type must be ADD or SUBTRACT")
    private String adjustmentType;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}

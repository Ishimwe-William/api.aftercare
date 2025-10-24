package com.bunsen.api.aftercare.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SparePartRequest {
    @NotBlank(message = "Part name is required")
    @Size(max = 100, message = "Part name must not exceed 100 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @DecimalMin(value = "0.0", message = "Quantity must be non-negative")
    private Double quantityAvailable;

    @NotNull(message = "Cost is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Cost must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Cost must have max 10 integer and 2 decimal digits")
    private BigDecimal cost;

    @Size(max = 100, message = "Supplier name must not exceed 100 characters")
    private String supplierName;

    @Size(max = 255, message = "Supplier contact must not exceed 255 characters")
    private String supplierContact;

    @DecimalMin(value = "0.0", message = "Low stock threshold must be non-negative")
    private Double lowStockThreshold;
}

package com.bunsen.api.aftercare.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class SparePartDTO {
    @Data
    public static class PartUsageRequest {
        @NotBlank(message = "Task ID is required")
        private String taskId;

        @NotBlank(message = "Part ID is required")
        private String partId;

        @NotNull(message = "Quantity used is required")
        @DecimalMin(value = "1.0", message = "Quantity must be at least 1")
        private Double quantityUsed;

        @Size(max = 500, message = "Notes must not exceed 500 characters")
        private String notes;
    }

    @Data
    public static class SparePartRequest {
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

    @Data
    public static class StockAdjustmentRequest {
        @NotNull(message = "Target quantity is required")
        @DecimalMin(value = "0.0", message = "Target quantity must be zero or greater")
        private Double targetQuantity;

        @Size(max = 500, message = "Reason must not exceed 500 characters")
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartUsageResponse {
        private String usageId;
        private String taskId;
        private String partId;
        private String partName;
        private Double quantityUsed;
        private BigDecimal cost;
        private String notes;
        private LocalDateTime usedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SparePartResponse {
        private String id;
        private String name;
        private String description;
        private Double quantityAvailable;
        private BigDecimal cost;
        private String supplierName;
        private String supplierContact;
        private Double lowStockThreshold;
        private Boolean isLowStock;
        private Boolean isOutOfStock;
        private Long totalUsed;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockAlertResponse {
        private Integer lowStockCount;
        private Integer outOfStockCount;
        private List<SparePartResponse> lowStockParts;
        private List<SparePartResponse> outOfStockParts;
        private LocalDateTime alertTimestamp;
    }
}

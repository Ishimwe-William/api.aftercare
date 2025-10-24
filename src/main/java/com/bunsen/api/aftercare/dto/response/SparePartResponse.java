package com.bunsen.api.aftercare.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SparePartResponse {
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

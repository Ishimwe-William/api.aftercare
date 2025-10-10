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
    private String partId;
    private String name;
    private String description;
    private Integer quantityAvailable;
    private BigDecimal cost;
    private String supplierName;
    private String supplierContact;
    private Integer lowStockThreshold;
    private Boolean isLowStock;
    private Boolean isOutOfStock;
    private Long totalUsed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

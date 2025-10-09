package com.bunsen.api.aftercare.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "spare_parts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SparePart {
    @Id
    @Column(name = "part_id", length = 36, nullable = false)
    private String partId;
    @Column(name = "name", length = 100, nullable = false)
    private String name;
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    @Column(name = "quantity_available", nullable = false)
    private Integer quantityAvailable = 0;
    @Column(name = "cost", precision = 10, scale = 2, nullable = false)
    private BigDecimal cost;
    @Column(name = "supplier_name", length = 100)
    private String supplierName;
    @Column(name = "supplier_contact", length = 255)
    private String supplierContact;
    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold = 10;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (quantityAvailable == null) {
            quantityAvailable = 0;
        }
        if (lowStockThreshold == null) {
            lowStockThreshold = 10;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
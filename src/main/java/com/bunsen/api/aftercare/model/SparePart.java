package com.bunsen.api.aftercare.model;

import com.bunsen.api.aftercare.model.base.BaseEntity;
import com.bunsen.api.aftercare.model.embedded.SupplierInfo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "spare_parts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SparePart extends BaseEntity {

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "quantity_available", nullable = false)
    private Double quantityAvailable = 0.0;

    @Column(name = "cost", precision = 10, scale = 2, nullable = false)
    private BigDecimal cost;

    @Embedded
    private SupplierInfo supplier;

    @Column(name = "low_stock_threshold")
    private Double lowStockThreshold = 10.0;

    @Override
    protected void onCreate() {
        super.onCreate();
        if (quantityAvailable == null) {
            quantityAvailable = 0.0;
        }
        if (lowStockThreshold == null) {
            lowStockThreshold = 10.0;
        }
    }
}
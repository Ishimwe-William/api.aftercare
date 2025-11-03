package com.bunsen.api.aftercare.model;

import com.bunsen.api.aftercare.model.base.TimestampedEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_line_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceLineItem extends TimestampedEntity {

    @Id
    @Column(name = "line_item_id", length = 36, nullable = false)
    private String lineItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    // Static snapshot of part information at time of invoice generation
    @Column(name = "part_id", length = 36)
    private String partId; // Reference to original part

    @Column(name = "part_name", length = 100, nullable = false)
    private String partName;

    @Column(name = "part_description", columnDefinition = "TEXT")
    private String partDescription;

    @Column(name = "quantity_used", nullable = false)
    private Double quantityUsed;

    @Column(name = "unit_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "total_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
package com.bunsen.api.aftercare.model;

import com.bunsen.api.aftercare.enums.EInvoiceStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int partsTotal;
    private int laborCost;
    private int totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EInvoiceStatus status;

    @OneToOne
    @JoinColumn(name = "service_case_id")
    private ServiceCase serviceCase;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
    }
}

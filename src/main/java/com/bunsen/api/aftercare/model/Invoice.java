package com.bunsen.api.aftercare.model;

import com.bunsen.api.aftercare.model.base.TimestampedEntity;
import com.bunsen.api.aftercare.model.embedded.OwnerInfo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Invoice extends TimestampedEntity {

    @Id
    @Column(name = "invoice_id", length = 36, nullable = false)
    private String invoiceId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private ServiceTask task;

    // Static snapshot data - not affected by future changes
    @Column(name = "motorcycle_model", length = 100)
    private String motorcycleModel;

    @Column(name = "motorcycle_phone", length = 100)
    private String motorcycleOwnerPhone;
    @Column(name = "motorcycle_owner_email", nullable = false)
    private String motorcycleOwnerEmail;
    @Column(name = "motorcycle_owner_name", length = 100)
    private String motorcycleOwnerName;

    @Column(name = "motorcycle_plate_number", length = 100)
    private String motorcyclePlateNumber;

    // Known Issue snapshot (replaces labor rate and hours)
    @Column(name = "known_issue_name")
    private String knownIssueName;

    @Column(name = "known_issue_price", precision = 10, scale = 2)
    private BigDecimal knownIssuePrice;

    @Column(name = "issue_cost", precision = 10, scale = 2)
    private BigDecimal issueCost;

    @Column(name = "technician_name", length = 255)
    private String technicianName;

    @Column(name = "issue_type", length = 100)
    private String issueType;

    @Column(name = "labor_hours", precision = 5, scale = 2)
    private BigDecimal laborHours;

    @Column(name = "labor_rate", precision = 10, scale = 2)
    private BigDecimal laborRate;

    @Column(name = "labor_cost", precision = 10, scale = 2)
    private BigDecimal laborCost;

    @Column(name = "parts_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal partsCost;

    @Column(name = "total_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "discount", precision = 10, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @Override
    protected void onCreate() {
        super.onCreate();
        if (generatedAt == null) {
            generatedAt = getTimestamp();
        }
        if (discount == null) {
            discount = BigDecimal.ZERO;
        }
    }
}
package com.bunsen.api.aftercare.model;

import com.bunsen.api.aftercare.enums.ETaskStatus;
import com.bunsen.api.aftercare.model.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_tasks",
        indexes = {
                @Index(name = "idx_status", columnList = "status"),
                @Index(name = "idx_technician_id", columnList = "technician_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTask extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motorcycle_id", nullable = false)
    private Motorcycle motorcycle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id", nullable = false)
    private User technician;

    @Column(name = "issue_type", length = 100, nullable = false)
    private String issueType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ETaskStatus status = ETaskStatus.PENDING;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "labor_hours", precision = 5, scale = 2)
    private BigDecimal laborHours = BigDecimal.ZERO;

    @Column(name = "estimated_time")
    private Integer estimatedTime;

    @Column(name = "due_time")
    private LocalDateTime dueTime;

      @Override
    protected void onCreate() {
        super.onCreate();
        if (status == null) {
            status = ETaskStatus.PENDING;
        }
        if (laborHours == null) {
            laborHours = BigDecimal.ZERO;
        }
    }
}
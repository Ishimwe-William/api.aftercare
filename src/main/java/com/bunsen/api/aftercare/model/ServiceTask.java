package com.bunsen.api.aftercare.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_tasks",
        indexes = {
                @Index(name = "idx_status", columnList = "status"),
                @Index(name = "idx_technician_id", columnList = "technician_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTask {
    @Id
    @Column(name = "task_id", length = 36, nullable = false)
    private String taskId;
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
    private TaskStatus status = TaskStatus.PENDING;
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    @Column(name = "labor_hours", precision = 5, scale = 2)
    private BigDecimal laborHours = BigDecimal.ZERO;
    @Column(name = "estimated_time")
    private Integer estimatedTime;
    @Column(name = "due_time")
    private LocalDateTime dueTime;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum TaskStatus {
        PENDING,
        IN_PROGRESS,
        PAUSED,
        COMPLETED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = TaskStatus.PENDING;
        }
        if (laborHours == null) {
            laborHours = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
package com.bunsen.api.aftercare.model;

import com.bunsen.api.aftercare.model.base.TimestampedEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_part_usages",
        uniqueConstraints = @UniqueConstraint(columnNames = {"task_id", "part_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskPartUsage extends TimestampedEntity {

    @Id
    @Column(name = "usage_id", length = 36, nullable = false)
    private String usageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private ServiceTask task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id", nullable = false)
    private SparePart part;

    @Column(name = "quantity_used", nullable = false)
    private Double quantityUsed;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "used_at", nullable = false, updatable = false)
    private LocalDateTime usedAt;

    @Override
    protected void onCreate() {
        super.onCreate();
        if (usedAt == null) {
            usedAt = getTimestamp();
        }
    }
}
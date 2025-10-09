package com.bunsen.api.aftercare.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs",
        indexes = {
                @Index(name = "idx_timestamp", columnList = "timestamp"),
                @Index(name = "idx_user_id", columnList = "user_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLog {
    @Id
    @Column(name = "log_id", length = 36, nullable = false)
    private String logId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    @Column(name = "action", length = 100, nullable = false)
    private String action;
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
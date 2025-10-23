package com.bunsen.api.aftercare.model;

import com.bunsen.api.aftercare.model.base.TimestampedEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "activity_logs",
        indexes = {
                @Index(name = "idx_timestamp", columnList = "timestamp"),
                @Index(name = "idx_user_id", columnList = "user_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLog extends TimestampedEntity {

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
}
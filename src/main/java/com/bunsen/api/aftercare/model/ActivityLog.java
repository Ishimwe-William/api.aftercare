package com.bunsen.api.aftercare.model;

import com.bunsen.api.aftercare.enums.EActivityAction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "activity_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EActivityAction action; // e.g. "Completed task", "Scanned QR"

    private String context; // task/bike/case/etc.

    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
    }
}

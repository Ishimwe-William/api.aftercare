package com.bunsen.api.aftercare.model;

import com.bunsen.api.aftercare.enums.EReportType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EReportType type; // e.g., "daily", "weekly"

    private String technicianName;

    private int completedTasks;
    private int hoursLogged;
    private int revenueGenerated;

    private String exportUrl;

    @Temporal(TemporalType.TIMESTAMP)
    private Date generatedAt = new Date();
}

package com.bunsen.api.aftercare.dto.response;

import com.bunsen.api.aftercare.model.ServiceTask;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTaskResponse {
    private String id;
    private String motorcycleId;
    private String motorcyclePlateNumber;
    private String technicianId;
    private String technicianName;
    private String issueType;
    private String description;
    private ServiceTask.TaskStatus status;
    private LocalDateTime assignedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String notes;
    private BigDecimal laborHours;
    private Integer estimatedTime;
    private LocalDateTime dueTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long durationInHours;
    private Boolean isOverdue;
}
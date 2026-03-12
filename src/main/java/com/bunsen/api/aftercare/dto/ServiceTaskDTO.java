package com.bunsen.api.aftercare.dto;

import com.bunsen.api.aftercare.enums.ETaskStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ServiceTaskDTO {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskStatusUpdateRequest {
        private ETaskStatus status;
        private String notes;
        private BigDecimal laborHours;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceTaskRequest {

        @NotBlank(message = "Motorcycle ID is required")
        private String motorcycleId;

        @NotBlank(message = "Technician ID is required")
        private String technicianId;

        @NotBlank(message = "Issue type is required")
        @Size(max = 100, message = "Issue type must not exceed 100 characters")
        private String issueType;

        @Size(max = 5000, message = "Description must not exceed 5000 characters")
        private String description;

        @Size(max = 5000, message = "Notes must not exceed 5000 characters")
        private String notes;

        @DecimalMin(value = "0.0", message = "Labor hours must be non-negative")
        @DecimalMax(value = "999.99", message = "Labor hours must not exceed 999.99")
        private BigDecimal laborHours;

        @Min(value = 0, message = "Estimated time must be non-negative")
        private Integer estimatedTime;

        private LocalDateTime dueTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceTaskResponse {
        private String id;
        private String motorcycleId;
        private String motorcyclePlateNumber;
        private String technicianId;
        private String technicianName;
        private String issueType;
        private String description;
        private ETaskStatus status;
        private LocalDateTime assignedAt;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private LocalDateTime cancelledAt;
        private String notes;
        private BigDecimal laborHours;
        private Integer estimatedTime;
        private LocalDateTime dueTime;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Long durationInHours;
        private Boolean isOverdue;
        private String priority;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskStatisticsResponse {
        private Long totalTasks;
        private Long pendingTasks;
        private Long inProgressTasks;
        private Long completedTasks;
        private Long overdueTasks;
        private Double averageCompletionTimeInHours;
        private Long totalLaborHours;
    }
}

package com.bunsen.api.aftercare.util;

import com.bunsen.api.aftercare.dto.ServiceTaskDTO;
import com.bunsen.api.aftercare.enums.ETaskStatus;
import com.bunsen.api.aftercare.model.*;
import com.bunsen.api.aftercare.service.helper.TaskPriorityCalculator;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Centralized mapper utility to avoid repetitive mapping logic
 */
@Component
public class EntityMapperUtil {

    private final TaskPriorityCalculator priorityCalculator;

    public EntityMapperUtil(TaskPriorityCalculator priorityCalculator) {
        this.priorityCalculator = priorityCalculator;
    }

    /**
     * Map ServiceTask to ServiceTaskResponse
     */
    public ServiceTaskDTO.ServiceTaskResponse mapToServiceTaskResponse(ServiceTask task) {
        Long durationInHours = calculateDuration(task.getStartedAt(), task.getCompletedAt());
        Boolean isOverdue = isTaskOverdue(task);

        return ServiceTaskDTO.ServiceTaskResponse.builder()
                .id(task.getId())
                .motorcycleId(task.getMotorcycle().getId())
                .motorcyclePlateNumber(task.getMotorcycle().getPlateNumber())
                .technicianId(task.getTechnician().getId())
                .technicianName(task.getTechnician().getFullName())
                .issueType(task.getIssueType())
                .description(task.getDescription())
                .status(task.getStatus())
                .assignedAt(task.getAssignedAt())
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                .cancelledAt(task.getCancelledAt()) // <-- ADDED
                .notes(task.getNotes())
                .laborHours(task.getLaborHours())
                .estimatedTime(task.getEstimatedTime())
                .dueTime(task.getDueTime())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .durationInHours(durationInHours)
                .isOverdue(isOverdue)
                .priority(priorityCalculator.determinePriority(task))
                .build();
    }

    /**
     * Calculate duration in hours
     */
    private Long calculateDuration(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null) {
            return Duration.between(start, end).toHours();
        }
        return null;
    }

    /**
     * Check if task is overdue
     */
    private Boolean isTaskOverdue(ServiceTask task) {
        if (task.getDueTime() != null &&
                (task.getStatus() == ETaskStatus.PENDING ||
                        task.getStatus() == ETaskStatus.IN_PROGRESS)) {
            return task.getDueTime().isBefore(LocalDateTime.now());
        }
        return false;
    }
}
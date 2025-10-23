package com.bunsen.api.aftercare.util;

import com.bunsen.api.aftercare.dto.response.*;
import com.bunsen.api.aftercare.model.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Centralized mapper utility to avoid repetitive mapping logic
 */
@Component
public class EntityMapperUtil {

    /**
     * Map User to TechnicianResponse
     */
    public TechnicianResponse mapToTechnicianResponse(User technician, Long activeTasks, Long completedTasks) {
        Set<String> roleNames = technician.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());

        return TechnicianResponse.builder()
                .id(technician.getId())
                .username(technician.getUsername())
                .email(technician.getEmail())
                .fullName(technician.getFullName())
                .phoneNumber(technician.getPhoneNumber())
                .photoUrl(technician.getPhotoUrl())
                .enabled(technician.isEnabled())
                .status(technician.isStatus())
                .roles(roleNames)
                .activeTasks(activeTasks)
                .completedTasks(completedTasks)
                .createdAt(technician.getCreatedAt())
                .updatedAt(technician.getUpdatedAt())
                .build();
    }

    /**
     * Map ServiceTask to ServiceTaskResponse
     */
    public ServiceTaskResponse mapToServiceTaskResponse(ServiceTask task) {
        Long durationInHours = calculateDuration(task.getStartedAt(), task.getCompletedAt());
        Boolean isOverdue = isTaskOverdue(task);

        return ServiceTaskResponse.builder()
                .taskId(task.getId())
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
                .notes(task.getNotes())
                .laborHours(task.getLaborHours())
                .estimatedTime(task.getEstimatedTime())
                .dueTime(task.getDueTime())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .durationInHours(durationInHours)
                .isOverdue(isOverdue)
                .build();
    }

    /**
     * Map Motorcycle to MotorcycleResponse
     */
    public MotorcycleResponse mapToMotorcycleResponse(Motorcycle motorcycle, int activeTasksCount, boolean needsService) {
        return MotorcycleResponse.builder()
                .id(motorcycle.getId())
                .qrCode(motorcycle.getQrCode())
                .model(motorcycle.getModel())
                .plateNumber(motorcycle.getPlateNumber())
                .ownerName(motorcycle.getOwner().getName())
                .ownerPhone(motorcycle.getOwner().getPhone())
                .ownerEmail(motorcycle.getOwner().getEmail())
                .status(motorcycle.getStatus())
                .lastServiceDate(motorcycle.getLastServiceDate())
                .createdAt(motorcycle.getCreatedAt())
                .updatedAt(motorcycle.getUpdatedAt())
                .activeTasksCount(activeTasksCount)
                .needsService(needsService)
                .build();
    }

    /**
     * Map SparePart to SparePartResponse
     */
    public SparePartResponse mapToSparePartResponse(SparePart part, Long totalUsed) {
        return SparePartResponse.builder()
                .partId(part.getId())
                .name(part.getName())
                .description(part.getDescription())
                .quantityAvailable(part.getQuantityAvailable())
                .cost(part.getCost())
                .supplierName(part.getSupplier().getName())
                .supplierContact(part.getSupplier().getContact())
                .lowStockThreshold(part.getLowStockThreshold())
                .isLowStock(part.getQuantityAvailable() <= part.getLowStockThreshold())
                .isOutOfStock(part.getQuantityAvailable() == 0)
                .totalUsed(totalUsed != null ? totalUsed : 0L)
                .createdAt(part.getCreatedAt())
                .updatedAt(part.getUpdatedAt())
                .build();
    }

    /**
     * Map TaskPartUsage to PartUsageResponse
     */
    public PartUsageResponse mapToPartUsageResponse(TaskPartUsage usage) {
        return PartUsageResponse.builder()
                .usageId(usage.getUsageId())
                .taskId(usage.getTask().getId())
                .partId(usage.getPart().getId())
                .partName(usage.getPart().getName())
                .quantityUsed(usage.getQuantityUsed())
                .notes(usage.getNotes())
                .usedAt(usage.getUsedAt())
                .build();
    }

    /**
     * Calculate task progress percentage
     */
    public Integer calculateTaskProgress(ServiceTask task) {
        switch (task.getStatus()) {
            case PENDING:
                return 0;
            case IN_PROGRESS:
                if (task.getEstimatedTime() != null && task.getStartedAt() != null) {
                    long elapsed = Duration.between(task.getStartedAt(), LocalDateTime.now()).toMinutes();
                    int progress = (int) ((elapsed * 100) / task.getEstimatedTime());
                    return Math.min(progress, 95);
                }
                return 50;
            case COMPLETED:
                return 100;
            case PAUSED:
                return 30;
            default:
                return 0;
        }
    }

    /**
     * Determine task priority
     */
    public String determineTaskPriority(ServiceTask task) {
        if (task.getDueTime() != null) {
            long hoursUntilDue = Duration.between(LocalDateTime.now(), task.getDueTime()).toHours();
            if (hoursUntilDue < 0) return "critical";
            if (hoursUntilDue < 4) return "high";
            if (hoursUntilDue < 24) return "medium";
        }
        return "low";
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
                (task.getStatus() == ServiceTask.TaskStatus.PENDING ||
                        task.getStatus() == ServiceTask.TaskStatus.IN_PROGRESS)) {
            return task.getDueTime().isBefore(LocalDateTime.now());
        }
        return false;
    }

    /**
     * Calculate total cost for part usage
     */
    public BigDecimal calculatePartUsageCost(int quantity, BigDecimal unitCost) {
        return unitCost.multiply(BigDecimal.valueOf(quantity));
    }
}
package com.bunsen.api.aftercare.service.helper;

import com.bunsen.api.aftercare.dto.MonitoringDTO.MonitoringFilter;
import com.bunsen.api.aftercare.exception.BadRequestException;
import com.bunsen.api.aftercare.exception.ValidationException;
import com.bunsen.api.aftercare.model.ServiceTask;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper component for filtering tasks based on various criteria
 * Centralizes filtering logic used across multiple services
 */
@Component
public class TaskFilterHelper {

    public List<ServiceTask> filterTasks(List<ServiceTask> tasks, MonitoringFilter filter) {
        if (filter == null) {
            return tasks;
        }

        return tasks.stream()
                .filter(task -> matchesFilter(task, filter))
                .collect(Collectors.toList());
    }

    public boolean matchesFilter(ServiceTask task, MonitoringFilter filter) {
        if (filter.getStatus() != null && task.getStatus() != filter.getStatus()) {
            return false;
        }

        if (filter.getTechnicianId() != null
                && !task.getTechnician().getId().equals(filter.getTechnicianId())) {
            return false;
        }

        if (filter.getMotorcycleId() != null
                && !task.getMotorcycle().getId().equals(filter.getMotorcycleId())) {
            return false;
        }

        if (filter.getStartDate() != null
                && task.getCreatedAt().isBefore(filter.getStartDate())) {
            return false;
        }

        return filter.getEndDate() == null
                || !task.getCreatedAt().isAfter(filter.getEndDate());
    }

    public void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new BadRequestException("Start date and end date are required");
        }

        if (startDate.isAfter(endDate)) {
            throw new ValidationException("Start date must be before end date");
        }
    }

    public void validateFilter(MonitoringFilter filter) {
        if (filter == null) {
            throw new BadRequestException("Filter parameters are required");
        }
    }
}
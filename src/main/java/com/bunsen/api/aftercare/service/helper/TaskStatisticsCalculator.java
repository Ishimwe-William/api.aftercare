package com.bunsen.api.aftercare.service.helper;

import com.bunsen.api.aftercare.enums.ETaskStatus;
import com.bunsen.api.aftercare.model.ServiceTask;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Utility component for calculating task statistics
 * Eliminates duplication between MonitoringService and ServiceTaskService
 */
@Component
public class TaskStatisticsCalculator {

    public TaskStatistics calculateStatistics(List<ServiceTask> tasks) {
        LocalDateTime now = LocalDateTime.now();

        long total = tasks.size();
        long pending = countByStatus(tasks, ETaskStatus.PENDING);
        long inProgress = countByStatus(tasks, ETaskStatus.IN_PROGRESS);
        long completed = countByStatus(tasks, ETaskStatus.COMPLETED);
        long overdue = countOverdueTasks(tasks, now);
        long totalLaborHours = calculateTotalLaborHours(tasks);

        return TaskStatistics.builder()
                .totalTasks(total)
                .pendingTasks(pending)
                .inProgressTasks(inProgress)
                .completedTasks(completed)
                .overdueTasks(overdue)
                .totalLaborHours(totalLaborHours)
                .build();
    }

    public long countByStatus(List<ServiceTask> tasks, ETaskStatus status) {
        return tasks.stream()
                .filter(t -> t.getStatus() == status)
                .count();
    }

    public long countOverdueTasks(List<ServiceTask> tasks, LocalDateTime referenceTime) {
        return tasks.stream()
                .filter(t -> isTaskOverdue(t, referenceTime))
                .count();
    }

    public boolean isTaskOverdue(ServiceTask task, LocalDateTime referenceTime) {
        return task.getDueTime() != null
                && task.getDueTime().isBefore(referenceTime)
                && (task.getStatus() == ETaskStatus.PENDING
                || task.getStatus() == ETaskStatus.IN_PROGRESS);
    }

    public long calculateTotalLaborHours(List<ServiceTask> tasks) {
        return tasks.stream()
                .map(ServiceTask::getLaborHours)
                .filter(Objects::nonNull)
                .mapToLong(BigDecimal::longValue)
                .sum();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TaskStatistics {
        private Long totalTasks;
        private Long pendingTasks;
        private Long inProgressTasks;
        private Long completedTasks;
        private Long overdueTasks;
        private Long totalLaborHours;
    }
}


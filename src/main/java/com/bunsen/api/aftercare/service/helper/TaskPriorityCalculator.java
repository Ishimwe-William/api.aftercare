package com.bunsen.api.aftercare.service.helper;

import com.bunsen.api.aftercare.enums.ETaskStatus;
import com.bunsen.api.aftercare.model.ServiceTask;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Component for calculating task priority and progress
 * Eliminates duplicate calculation logic
 */
@Component
public class TaskPriorityCalculator {

    public String determinePriority(ServiceTask task) {
        if (task.getDueTime() != null) {
            long hoursUntilDue = ChronoUnit.HOURS.between(LocalDateTime.now(), task.getDueTime());

            if (hoursUntilDue < 0) {
                return "critical";
            }
            if (hoursUntilDue < 4) {
                return "high";
            }
            if (hoursUntilDue < 24) {
                return "medium";
            }
        }
        return "low";
    }

    public Integer calculateProgress(ServiceTask task) {
        return switch (task.getStatus()) {
            case IN_PROGRESS -> {
                if (task.getEstimatedTime() != null && task.getStartedAt() != null) {
                    long elapsed = ChronoUnit.MINUTES.between(
                            task.getStartedAt(),
                            LocalDateTime.now()
                    );
                    int progress = (int) ((elapsed * 100) / task.getEstimatedTime());
                    yield Math.min(progress, 95);
                }
                yield 50;
            }
            case COMPLETED -> 100;
            case PAUSED -> 30;
            default -> 0;
        };
    }

    public long calculateHoursOverdue(ServiceTask task, LocalDateTime referenceTime) {
        if (task.getDueTime() == null || !task.getDueTime().isBefore(referenceTime)) {
            return 0;
        }
        return ChronoUnit.HOURS.between(task.getDueTime(), referenceTime);
    }

    public boolean shouldGenerateAlert(ServiceTask task, LocalDateTime referenceTime) {
        // High priority if due within 2 hours
        return task.getStatus() == ETaskStatus.IN_PROGRESS
                && task.getDueTime() != null
                && task.getDueTime().isBefore(referenceTime.plusHours(2));
    }

    public String getAlertSeverity(long hoursOverdue) {
        return hoursOverdue > 24 ? "error" : "warning";
    }
}
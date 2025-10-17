package com.bunsen.api.aftercare.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicianPerformanceResponse {
    private String technicianId;
    private String username;
    private String fullName;
    private Long totalTasksCompleted;
    private Long activeTasks;
    private Long pendingTasks;
    private Long inProgressTasks;
    private Double averageCompletionTimeHours;
    private Double efficiencyScore;
}

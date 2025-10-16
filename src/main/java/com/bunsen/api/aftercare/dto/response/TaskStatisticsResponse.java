package com.bunsen.api.aftercare.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatisticsResponse {
    private Long totalTasks;
    private Long pendingTasks;
    private Long inProgressTasks;
    private Long completedTasks;
    private Long overdueTasks;
    private Double averageCompletionTimeInHours;
    private Long totalLaborHours;
}
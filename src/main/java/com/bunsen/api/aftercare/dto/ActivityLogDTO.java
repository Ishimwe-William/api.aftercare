package com.bunsen.api.aftercare.dto;

import com.bunsen.api.aftercare.model.ActivityLog;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogDTO {
    private String logId;
    private String userId;
    private String userName;
    private String action;
    private String details;
    private LocalDateTime timestamp;

    public static ActivityLogDTO fromEntity(ActivityLog log) {
        return new ActivityLogDTO(
                log.getLogId(),
                log.getUser() != null ? log.getUser().getId() : null,
                log.getUser() != null ? log.getUser().getFullName() : "Unknown",
                log.getAction(),
                log.getDetails(),
                log.getTimestamp()
        );
    }
}
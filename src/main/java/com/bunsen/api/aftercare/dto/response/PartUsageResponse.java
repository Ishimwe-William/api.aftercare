package com.bunsen.api.aftercare.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartUsageResponse {
    private String usageId;
    private String taskId;
    private String partId;
    private String partName;
    private Integer quantityUsed;
    private String notes;
    private LocalDateTime usedAt;

}

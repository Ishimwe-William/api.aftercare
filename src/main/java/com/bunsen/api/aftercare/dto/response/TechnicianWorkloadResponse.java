package com.bunsen.api.aftercare.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicianWorkloadResponse {
    private String technicianId;
    private String username;
    private String fullName;
    private Long activeTasksCount;
    private boolean available;
    private String status;
}

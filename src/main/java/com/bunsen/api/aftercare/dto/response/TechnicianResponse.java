package com.bunsen.api.aftercare.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicianResponse {
    private String id;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String photoUrl;
    private boolean enabled;
    private boolean status;
    private Set<String> roles;
    private Long activeTasks;
    private Long completedTasks;
    private Double averageCompletionTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

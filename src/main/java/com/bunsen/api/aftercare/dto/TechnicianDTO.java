package com.bunsen.api.aftercare.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

public class TechnicianDTO {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TechnicianPerformanceResponse {
        private String technicianId;
        private String username;
        private String fullName;
        private Long totalTasksCompleted;
        private Long activeTasks;
        private Long pendingTasks;
        private Long inProgressTasks;
        private Long overdueTasksCount;
        private Long onTimeTasksCount;
        private Double averageDelayHours;
        private Double onTimeCompletionRate;
        private Double averageCompletionTimeHours;
        private Double efficiencyScore;
    }
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechnicianRequest {

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
        private String username;

        @NotBlank(message = "Email is required")
        @Size(max = 50, message = "Email must not exceed 50 characters")
        @Email(message = "Email must be valid")
        private String email;

        @Size(max = 100, message = "Full name must not exceed 100 characters")
        private String fullName;

        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be valid")
        private String phoneNumber;

        private String photoUrl;

        private boolean enabled = true;

        private boolean status = true;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TechnicianUpdateRequest {

        @Size(max = 50, message = "Email must not exceed 50 characters")
        @Email(message = "Email must be valid")
        private String email;

        @Size(max = 100, message = "Full name must not exceed 100 characters")
        private String fullName;

        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be valid")
        private String phoneNumber;

        private String photoUrl;

        private Boolean enabled;

        private Boolean status;
    }
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TechnicianResponse {
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
        private Double averageCompletionTimeHours;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TechnicianWorkloadResponse {
        private String technicianId;
        private String username;
        private String fullName;
        private Long activeTasksCount;
        private boolean available;
        private String status;
    }

}

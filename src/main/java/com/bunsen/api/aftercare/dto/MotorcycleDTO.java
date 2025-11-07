package com.bunsen.api.aftercare.dto;

import com.bunsen.api.aftercare.model.Motorcycle;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.common.aliasing.qual.Unique;

import java.time.LocalDateTime;

public class MotorcycleDTO {
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MotorcycleRequest {

        @NotBlank(message = "QR code is required")
        @Size(max = 255, message = "QR code must not exceed 255 characters")
        @Unique
        private String qrCode;

        @NotBlank(message = "Model is required")
        @Size(max = 100, message = "Model must not exceed 100 characters")
        private String model;

        @Size(max = 100, message = "Plate number must not exceed 100 characters")
        @Unique
        private String plateNumber;

        @Size(max = 100, message = "Owner name must not exceed 100 characters")
        private String ownerName;

        @Size(max = 20, message = "Owner phone must not exceed 20 characters")
        private String ownerPhone;

        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Owner email must not exceed 255 characters")
        private String ownerEmail;

        private LocalDateTime lastServiceDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MotorcycleStatusUpdateRequest {

        @NotNull(message = "Status is required")
        private Motorcycle.MotorcycleStatus status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MotorcycleResponse {
        private String id;
        private String qrCode;
        private String model;
        private String plateNumber;
        private String ownerName;
        private String ownerPhone;
        private String ownerEmail;
        private Motorcycle.MotorcycleStatus status;
        private LocalDateTime lastServiceDate;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Integer activeTasksCount;
        private Boolean needsService;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MotorcycleStatisticsResponse {
        private Long totalMotorcycles;
        private Long activeMotorcycles;
        private Long inactiveMotorcycles;
        private Long inServiceMotorcycles;
        private Long motorcyclesNeedingService;
    }

}

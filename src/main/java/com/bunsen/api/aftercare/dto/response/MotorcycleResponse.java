package com.bunsen.api.aftercare.dto.response;

import com.bunsen.api.aftercare.model.Motorcycle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MotorcycleResponse {
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


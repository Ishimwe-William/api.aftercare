package com.bunsen.api.aftercare.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTaskRequest {

    @NotBlank(message = "Motorcycle ID is required")
    private String motorcycleId;

    @NotBlank(message = "Technician ID is required")
    private String technicianId;

    @NotBlank(message = "Issue type is required")
    @Size(max = 100, message = "Issue type must not exceed 100 characters")
    private String issueType;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @Size(max = 5000, message = "Notes must not exceed 5000 characters")
    private String notes;

    @DecimalMin(value = "0.0", message = "Labor hours must be non-negative")
    @DecimalMax(value = "999.99", message = "Labor hours must not exceed 999.99")
    private BigDecimal laborHours;

    @Min(value = 0, message = "Estimated time must be non-negative")
    private Integer estimatedTime;

    private LocalDateTime dueTime;
}
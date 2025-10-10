package com.bunsen.api.aftercare.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PartUsageRequest {
    @NotBlank(message = "Task ID is required")
    private String taskId;

    @NotBlank(message = "Part ID is required")
    private String partId;

    @NotNull(message = "Quantity used is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantityUsed;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}

package com.bunsen.api.aftercare.dto.request;

import com.bunsen.api.aftercare.model.ServiceTask;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private ServiceTask.TaskStatus status;

    private String notes;

    private BigDecimal laborHours;
}
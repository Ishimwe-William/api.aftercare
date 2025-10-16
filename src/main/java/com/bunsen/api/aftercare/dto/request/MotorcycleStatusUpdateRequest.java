package com.bunsen.api.aftercare.dto.request;

import com.bunsen.api.aftercare.model.Motorcycle;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MotorcycleStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private Motorcycle.MotorcycleStatus status;
}

package com.bunsen.api.aftercare.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MotorcycleStatisticsResponse {
    private Long totalMotorcycles;
    private Long activeMotorcycles;
    private Long inactiveMotorcycles;
    private Long inServiceMotorcycles;
    private Long motorcyclesNeedingService;
}

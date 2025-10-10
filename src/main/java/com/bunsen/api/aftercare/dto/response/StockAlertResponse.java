package com.bunsen.api.aftercare.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAlertResponse {
    private Integer lowStockCount;
    private Integer outOfStockCount;
    private List<SparePartResponse> lowStockParts;
    private List<SparePartResponse> outOfStockParts;
    private LocalDateTime alertTimestamp;
}

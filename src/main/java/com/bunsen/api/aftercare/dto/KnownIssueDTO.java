package com.bunsen.api.aftercare.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

public class KnownIssueDTO {
    @Data
    public static class CreateRequest {
        private String name;
        private BigDecimal price;
    }

    @Data
    public static class UpdateRequest {
        private BigDecimal price;
    }

    @Data
    public static class Response {
        private Long id;
        private String name;
        private BigDecimal price;
        private Instant createdAt;
        private Instant updatedAt;
    }
}

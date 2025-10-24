package com.bunsen.api.aftercare.dto;

import com.bunsen.api.aftercare.model.ServiceTask;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class MonitoringDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServiceCaseResponse {
        private String caseId;
        private MotorcycleInfo motorcycle;
        private String issue;
        private String issueType;
        private String description;
        private String technician;
        private String technicianId;
        private ServiceTask.TaskStatus status;
        private Integer progress;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String priority;
        private BigDecimal laborHours;
        private Integer estimatedTime;
        private LocalDateTime dueTime;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MotorcycleInfo {
        private String id;
        private String model;
        private String plateNumber;
        private String qrCode;
        private String ownerName;
        private String ownerPhone;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AlertResponse {
        private String id;
        private String message;
        private String severity;
        private LocalDateTime timestamp;
        private String caseId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServiceTimelineData {
        private LocalDateTime timestamp;
        private Integer pendingCount;
        private Integer inProgressCount;
        private Integer completedCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PeakHoursData {
        private Integer hour;
        private Integer dayOfWeek;
        private Long taskCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonitoringStats {
        private Long totalCases;
        private Long pendingCases;
        private Long inProgressCases;
        private Long completedCases;
        private Long overdueCases;
        private Double averageCompletionTime;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReassignRequest {
        private String taskId;
        private String newTechnicianId;
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonitoringFilter {
        private ServiceTask.TaskStatus status;
        private String dateRange;
        private String technicianId;
        private String motorcycleId;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CaseDetailsResponse {
        private ServiceCaseResponse caseInfo;
        private List<PartUsageInfo> partsUsed;
        private List<ActivityInfo> activityHistory;
        private InvoiceInfo invoice;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PartUsageInfo {
        private String partId;
        private String partName;
        private Double quantityUsed;
        private BigDecimal unitCost;
        private BigDecimal totalCost;
        private LocalDateTime usedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActivityInfo {
        private String action;
        private String details;
        private LocalDateTime timestamp;
        private String performedBy;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InvoiceInfo {
        private String invoiceId;
        private String invoiceNumber;
        private BigDecimal laborCost;
        private BigDecimal partsCost;
        private BigDecimal totalCost;
        private BigDecimal discount;
        private LocalDateTime generatedAt;
    }
}
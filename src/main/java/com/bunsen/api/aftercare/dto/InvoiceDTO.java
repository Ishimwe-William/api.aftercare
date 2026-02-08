package com.bunsen.api.aftercare.dto;

import com.bunsen.api.aftercare.model.Invoice;
import com.bunsen.api.aftercare.model.InvoiceLineItem;
import com.bunsen.api.aftercare.model.embedded.OwnerInfo;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class InvoiceDTO {
    @Data
    public static class InvoiceRequest {

        @NotBlank(message = "Task ID is required")
        private String taskId;

        @NotNull(message = "Labor cost is required")
        @DecimalMin(value = "0.0", message = "Labor cost must be non-negative")
        private BigDecimal laborCost;

        @NotNull(message = "Parts cost is required")
        @DecimalMin(value = "0.0", message = "Parts cost must be non-negative")
        private BigDecimal partsCost;

        @DecimalMin(value = "0.0", message = "Discount must be non-negative")
        private BigDecimal discount;

        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceResponse {
        private String invoiceId;
        private String taskId;

        // Static snapshot data
        private String motorcycleModel;
        private String motorcyclePlateNumber;
        private String ownerEmail ;
        private String ownerName ;
        private String ownerPhone ;
        private String technicianName;
        private String issueType;

        // Known Issue details (snapshot)
        private String knownIssueName;
        private BigDecimal knownIssuePrice;
        private BigDecimal issueCost;

        private BigDecimal laborHours;
        private BigDecimal laborRate;
        private BigDecimal laborCost;
        private BigDecimal partsCost;
        private BigDecimal totalCost;
        private BigDecimal discount;
        private String notes;
        private LocalDateTime generatedAt;

        // Line items for parts
        private List<InvoiceLineItemResponse> lineItems;

        public static InvoiceResponse fromEntity(Invoice invoice) {
            return InvoiceResponse.builder()
                    .invoiceId(invoice.getInvoiceId())
                    .taskId(invoice.getTask().getId())
                    .ownerEmail(invoice.getMotorcycleOwnerEmail())
                    .ownerPhone(invoice.getMotorcycleOwnerPhone())
                    .ownerName(invoice.getMotorcycleOwnerName())
                    .motorcycleModel(invoice.getMotorcycleModel())
                    .motorcyclePlateNumber(invoice.getMotorcyclePlateNumber())
                    .technicianName(invoice.getTechnicianName())
                    .issueType(invoice.getIssueType())
                    .knownIssueName(invoice.getKnownIssueName())
                    .knownIssuePrice(invoice.getKnownIssuePrice())
                    .issueCost(invoice.getIssueCost())
                    .laborHours(invoice.getLaborHours())
                    .laborRate(invoice.getLaborRate())
                    .laborCost(invoice.getLaborCost())
                    .partsCost(invoice.getPartsCost())
                    .totalCost(invoice.getTotalCost())
                    .discount(invoice.getDiscount())
                    .notes(invoice.getNotes())
                    .generatedAt(invoice.getGeneratedAt())
                    .build();
        }
    }
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceLineItemResponse {
        private String lineItemId;
        private String invoiceId;
        private String partId;
        private String partName;
        private String partDescription;
        private Double quantityUsed;
        private BigDecimal unitCost;
        private BigDecimal totalCost;
        private String notes;

        public static InvoiceLineItemResponse fromEntity(InvoiceLineItem lineItem) {
            return InvoiceLineItemResponse.builder()
                    .lineItemId(lineItem.getLineItemId())
                    .invoiceId(lineItem.getInvoice().getInvoiceId())
                    .partId(lineItem.getPartId())
                    .partName(lineItem.getPartName())
                    .partDescription(lineItem.getPartDescription())
                    .quantityUsed(lineItem.getQuantityUsed())
                    .unitCost(lineItem.getUnitCost())
                    .totalCost(lineItem.getTotalCost())
                    .notes(lineItem.getNotes())
                    .build();
        }
    }

}

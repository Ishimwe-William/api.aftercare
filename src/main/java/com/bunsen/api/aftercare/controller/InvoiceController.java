package com.bunsen.api.aftercare.controller;

import com.bunsen.api.aftercare.dto.InvoiceDTO.*;
import com.bunsen.api.aftercare.model.Invoice;
import com.bunsen.api.aftercare.model.InvoiceLineItem;
import com.bunsen.api.aftercare.service.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/invoices")
@PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    /**
     * Matches: invoiceSlice.js -> generateInvoice
     * Updated to work with Known Issues instead of Labor Rates
     */
    @PostMapping
    public ResponseEntity<InvoiceResponse> createInvoice(@Valid @RequestBody InvoiceRequest request) {
        // Set a default labor cost if not provided
        Invoice invoice = invoiceService.generateInvoice(
                request.getTaskId(),
                request.getPartsCost(),
                request.getDiscount(),
                request.getNotes(),
                BigDecimal.ZERO
        );

        InvoiceResponse response = InvoiceResponse.fromEntity(invoice);

        // Add line items
        List<InvoiceLineItem> lineItems = invoiceService.getInvoiceLineItems(invoice.getInvoiceId());
        response.setLineItems(lineItems.stream()
                .map(InvoiceLineItemResponse::fromEntity)
                .collect(Collectors.toList()));

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Matches: invoiceSlice.js -> fetchInvoiceById
     */
    @GetMapping("/{invoiceId}")
    public ResponseEntity<InvoiceResponse> getInvoiceById(@PathVariable String invoiceId) {
        Invoice invoice = invoiceService.getInvoiceById(invoiceId);
        InvoiceResponse response = InvoiceResponse.fromEntity(invoice);

        // Add line items
        List<InvoiceLineItem> lineItems = invoiceService.getInvoiceLineItems(invoiceId);
        response.setLineItems(lineItems.stream()
                .map(InvoiceLineItemResponse::fromEntity)
                .collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

    /**
     * Matches: invoiceSlice.js -> fetchTaskInvoices
     */
    @GetMapping("/task/{taskId}")
    public ResponseEntity<InvoiceResponse> getInvoiceByTaskId(@PathVariable String taskId) {
        Invoice invoice = invoiceService.getInvoiceByTaskId(taskId);
        InvoiceResponse response = InvoiceResponse.fromEntity(invoice);

        // Add line items
        List<InvoiceLineItem> lineItems = invoiceService.getInvoiceLineItems(invoice.getInvoiceId());
        response.setLineItems(lineItems.stream()
                .map(InvoiceLineItemResponse::fromEntity)
                .collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

    /**
     * Get line items for a specific invoice
     */
    @GetMapping("/{invoiceId}/line-items")
    public ResponseEntity<List<InvoiceLineItemResponse>> getInvoiceLineItems(@PathVariable String invoiceId) {
        List<InvoiceLineItem> lineItems = invoiceService.getInvoiceLineItems(invoiceId);
        return ResponseEntity.ok(lineItems.stream()
                .map(InvoiceLineItemResponse::fromEntity)
                .collect(Collectors.toList()));
    }

    /**
     * Exposes the getInvoicesBetweenDates service method.
     */
    @GetMapping("/date-range")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<InvoiceResponse>> getInvoicesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Pageable pageable) {

        Page<Invoice> invoicePage = invoiceService.getInvoicesBetweenDates(startDate, endDate, pageable);
        return ResponseEntity.ok(invoicePage.map(InvoiceResponse::fromEntity));
    }

    /**
     * To update an existing invoice.
     * Updated to work with Known Issues (no laborCost parameter)
     */
    @PutMapping("/{invoiceId}")
    public ResponseEntity<InvoiceResponse> updateInvoice(
            @PathVariable String invoiceId,
            @Valid @RequestBody InvoiceRequest request) {

        Invoice invoice = invoiceService.updateInvoice(
                invoiceId,
                request.getPartsCost(),
                request.getDiscount(),
                request.getNotes()
        );

        InvoiceResponse response = InvoiceResponse.fromEntity(invoice);

        // Add line items
        List<InvoiceLineItem> lineItems = invoiceService.getInvoiceLineItems(invoiceId);
        response.setLineItems(lineItems.stream()
                .map(InvoiceLineItemResponse::fromEntity)
                .collect(Collectors.toList()));

        return ResponseEntity.ok(response);
    }

    /**
     * Exposes the calculateTotalRevenue service method.
     */
    @GetMapping("/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, BigDecimal>> getTotalRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        BigDecimal revenue = invoiceService.calculateTotalRevenue(startDate, endDate);
        return ResponseEntity.ok(Map.of("totalRevenue", revenue));
    }
}
package com.bunsen.api.aftercare.controller;

import com.bunsen.api.aftercare.dto.SparePartDTO.*;
import com.bunsen.api.aftercare.dto.response.MessageResponse;
import com.bunsen.api.aftercare.service.SparePartService;
import com.bunsen.api.aftercare.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/spare-parts")
@PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
public class SparePartController {

    private final SparePartService sparePartService;

    public SparePartController(SparePartService sparePartService) {
        this.sparePartService = sparePartService;
    }

    @GetMapping
    public ResponseEntity<Page<SparePartResponse>> getAllParts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(sparePartService.getAllParts(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SparePartResponse> getPartById(@PathVariable String id) {
        return ResponseEntity.ok(sparePartService.getPartById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<SparePartResponse>> searchParts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(sparePartService.searchByName(keyword, pageable));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<Page<SparePartResponse>> getLowStockParts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(sparePartService.getLowStockParts(pageable));
    }

    @GetMapping("/out-of-stock")
    public ResponseEntity<Page<SparePartResponse>> getOutOfStockParts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(sparePartService.getOutOfStockParts(pageable));
    }

    @GetMapping("/alerts")
    public ResponseEntity<StockAlertResponse> getStockAlerts() {
        return ResponseEntity.ok(sparePartService.getStockAlerts());
    }

    @GetMapping("/supplier/{supplierName}")
    public ResponseEntity<List<SparePartResponse>> getPartsBySupplier(@PathVariable String supplierName) {
        return ResponseEntity.ok(sparePartService.getPartsBySupplier(supplierName));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SparePartResponse> createPart(@Valid @RequestBody SparePartRequest request,
                                                        @AuthenticationPrincipal UserDetailsImpl principal) {
        String creatorId = principal.getId();
        SparePartResponse response = sparePartService.createPart(request, creatorId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SparePartResponse> updatePart(
            @PathVariable String id,
            @Valid @RequestBody SparePartRequest request) {
        return ResponseEntity.ok(sparePartService.updatePart(id, request));
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<SparePartResponse> adjustStock(
            @PathVariable String id,
            @Valid @RequestBody StockAdjustmentRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        String updaterId = principal.getId();
        return ResponseEntity.ok(sparePartService.adjustStock(id, request, updaterId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deletePart(@PathVariable String id,
                                                      @AuthenticationPrincipal UserDetailsImpl principal) {
        String deleterId = principal.getId();
        sparePartService.deletePart(id, deleterId);
        return ResponseEntity.ok(new MessageResponse("Spare part deleted successfully"));
    }

    @PostMapping("/usage")
    public ResponseEntity<PartUsageResponse> logPartUsage(@Valid @RequestBody PartUsageRequest request) {
        return ResponseEntity.ok(sparePartService.logPartUsage(request));
    }

    @GetMapping("/usage/{partId}")
    public ResponseEntity<List<PartUsageResponse>> getPartUsageHistory(@PathVariable String partId) {
        return ResponseEntity.ok(sparePartService.getPartUsageHistory(partId));
    }

    @GetMapping("/usage/task/{taskId}")
    public ResponseEntity<List<PartUsageResponse>> getTaskPartUsages(@PathVariable String taskId) {
        return ResponseEntity.ok(sparePartService.getTaskPartUsages(taskId));
    }

    @GetMapping("/statistics/most-used")
    public ResponseEntity<List<Map<String, Object>>> getMostUsedParts() {
        return ResponseEntity.ok(sparePartService.getMostUsedParts());
    }

    @PatchMapping("/usage/{usageId}")
    public ResponseEntity<PartUsageResponse> updatePartUsage(
            @PathVariable String usageId,
            @Valid @RequestBody PartUsageRequest request) {
        return ResponseEntity.ok(sparePartService.updatePartUsage(usageId, request));
    }

    @DeleteMapping("/usage/{usageId}")
    public ResponseEntity<MessageResponse> deletePartUsage(@PathVariable String usageId) {
        sparePartService.deletePartUsage(usageId);
        return ResponseEntity.ok(new MessageResponse("Part usage deleted successfully"));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportInventoryReport() {
        byte[] report = sparePartService.exportInventoryReport();
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=inventory_report.csv")
                .body(report);
    }
}
package com.bunsen.api.aftercare.controller;

import com.bunsen.api.aftercare.dto.MonitoringDTO.*;
import com.bunsen.api.aftercare.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class MonitoringController {

    private final MonitoringService monitoringService;

    @GetMapping("/cases")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR') or hasRole('TECHNICIAN')")
    public ResponseEntity<Page<ServiceCaseResponse>> getServiceCases(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String technicianId,
            @RequestParam(required = false) String motorcycleId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        MonitoringFilter filter = new MonitoringFilter();
        if (status != null) {
            filter.setStatus(com.bunsen.api.aftercare.model.ServiceTask.TaskStatus.valueOf(status.toUpperCase()));
        }
        filter.setTechnicianId(technicianId);
        filter.setMotorcycleId(motorcycleId);
        filter.setStartDate(startDate);
        filter.setEndDate(endDate);

        Sort sort = Sort.by(sortDir.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ServiceCaseResponse> cases = monitoringService.getServiceCases(filter, pageable);
        return ResponseEntity.ok(cases);
    }

    @GetMapping("/cases/filtered")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
    public ResponseEntity<List<ServiceCaseResponse>> getFilteredServiceCases(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String technicianId,
            @RequestParam(required = false) String motorcycleId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        MonitoringFilter filter = new MonitoringFilter();
        if (status != null) {
            filter.setStatus(com.bunsen.api.aftercare.model.ServiceTask.TaskStatus.valueOf(status.toUpperCase()));
        }
        filter.setTechnicianId(technicianId);
        filter.setMotorcycleId(motorcycleId);
        filter.setStartDate(startDate);
        filter.setEndDate(endDate);

        List<ServiceCaseResponse> cases = monitoringService.getFilteredServiceCases(filter);
        return ResponseEntity.ok(cases);
    }

    @GetMapping("/cases/{taskId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR') or hasRole('TECHNICIAN')")
    public ResponseEntity<CaseDetailsResponse> getCaseDetails(@PathVariable String taskId) {
        CaseDetailsResponse details = monitoringService.getCaseDetails(taskId);
        return ResponseEntity.ok(details);
    }

    @PostMapping("/cases/reassign")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
    public ResponseEntity<ServiceCaseResponse> reassignTask(@RequestBody ReassignRequest request) {
        ServiceCaseResponse response = monitoringService.reassignTask(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
    public ResponseEntity<List<AlertResponse>> getAlerts() {
        List<AlertResponse> alerts = monitoringService.getAlerts();
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
    public ResponseEntity<MonitoringStats> getStatistics() {
        MonitoringStats stats = monitoringService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/timeline")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
    public ResponseEntity<List<ServiceTimelineData>> getServiceTimeline(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        List<ServiceTimelineData> timeline = monitoringService.getServiceTimeline(startDate, endDate);
        return ResponseEntity.ok(timeline);
    }

    @GetMapping("/peak-hours")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
    public ResponseEntity<List<PeakHoursData>> getPeakHoursData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        List<PeakHoursData> peakHours = monitoringService.getPeakHoursData(startDate, endDate);
        return ResponseEntity.ok(peakHours);
    }
}
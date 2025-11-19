package com.bunsen.api.aftercare.controller;

import com.bunsen.api.aftercare.dto.ServiceTaskDTO.*;
import com.bunsen.api.aftercare.dto.response.MessageResponse;
import com.bunsen.api.aftercare.enums.ETaskStatus;
import com.bunsen.api.aftercare.service.ServiceTaskService;
import com.bunsen.api.aftercare.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/service-tasks")
@PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
public class ServiceTaskController {

    private final ServiceTaskService serviceTaskService;

    public ServiceTaskController(ServiceTaskService serviceTaskService) {
        this.serviceTaskService = serviceTaskService;
    }

    @GetMapping
    public ResponseEntity<Page<ServiceTaskResponse>> getAllTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(serviceTaskService.getAllTasks(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceTaskResponse> getTaskById(@PathVariable String id) {
        return ResponseEntity.ok(serviceTaskService.getTaskById(id));
    }

    @GetMapping("/technician/{technicianId}")
    public ResponseEntity<List<ServiceTaskResponse>> getTasksByTechnician(
            @PathVariable String technicianId) {
        return ResponseEntity.ok(serviceTaskService.getTasksByTechnician(technicianId));
    }

    @GetMapping("/motorcycle/{motorcycleId}")
    public ResponseEntity<List<ServiceTaskResponse>> getTasksByMotorcycle(
            @PathVariable String motorcycleId) {
        return ResponseEntity.ok(serviceTaskService.getTasksByMotorcycle(motorcycleId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ServiceTaskResponse>> getTasksByStatus(
            @PathVariable ETaskStatus status) {
        return ResponseEntity.ok(serviceTaskService.getTasksByStatus(status));
    }

    @GetMapping("/technician/{technicianId}/status/{status}")
    public ResponseEntity<List<ServiceTaskResponse>> getTasksByTechnicianAndStatus(
            @PathVariable String technicianId,
            @PathVariable ETaskStatus status) {
        return ResponseEntity.ok(serviceTaskService.getTasksByTechnicianAndStatus(technicianId, status));
    }

    @GetMapping("/overdue")
    public ResponseEntity<Page<ServiceTaskResponse>> getOverdueTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(serviceTaskService.getOverdueTasks(pageable));
    }

    @GetMapping("/completed")
    public ResponseEntity<Page<ServiceTaskResponse>> getCompletedTasksBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(serviceTaskService.getCompletedTasksBetween(startDate, endDate, pageable));
    }

    @GetMapping("/technician/{technicianId}/date-range")
    public ResponseEntity<Page<ServiceTaskResponse>> getTechnicianTasksInDateRange(
            @PathVariable String technicianId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(serviceTaskService.getTechnicianTasksInDateRange(
                technicianId, startDate, endDate, pageable));
    }

    @GetMapping("/date-range")
    public ResponseEntity<Page<ServiceTaskResponse>> getAllTasksInDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(serviceTaskService.getAllTasksInDateRange(startDate, endDate, pageable));
    }

    @GetMapping("/statistics")
    public ResponseEntity<TaskStatisticsResponse> getTaskStatistics() {
        return ResponseEntity.ok(serviceTaskService.getTaskStatistics());
    }

    @GetMapping("/technician/{technicianId}/completed-count")
    public ResponseEntity<Long> getCompletedTaskCountByTechnician(@PathVariable String technicianId) {
        return ResponseEntity.ok(serviceTaskService.getCompletedTaskCountByTechnician(technicianId));
    }

    @PostMapping
    public ResponseEntity<ServiceTaskResponse> createTask(
            @Valid @RequestBody ServiceTaskRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        String creatorId = principal.getId();
        ServiceTaskResponse response = serviceTaskService.createTask(request, creatorId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceTaskResponse> updateTask(
            @PathVariable String id,
            @Valid @RequestBody ServiceTaskRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(serviceTaskService.updateTask(id, request, principal));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ServiceTaskResponse> updateTaskStatus(
            @PathVariable String id,
            @Valid @RequestBody TaskStatusUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(serviceTaskService.updateTaskStatus(id, request, principal));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteTask(@PathVariable String id,
                                                      @AuthenticationPrincipal UserDetailsImpl principal) {

        serviceTaskService.deleteTask(id, principal);
        return ResponseEntity.ok(new MessageResponse("Service task deleted successfully"));
    }
}
package com.bunsen.api.aftercare.controller;

import com.bunsen.api.aftercare.dto.TechnicianDTO.*;
import com.bunsen.api.aftercare.dto.response.MessageResponse;
import com.bunsen.api.aftercare.service.TechnicianService;
import com.bunsen.api.aftercare.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/technicians")
@PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
public class TechnicianController {

    private final TechnicianService technicianService;

    public TechnicianController(TechnicianService technicianService) {
        this.technicianService = technicianService;
    }

    /**
     * Get all technicians
     * GET /api/technicians
     */
    @GetMapping
    public ResponseEntity<List<TechnicianResponse>> getAllTechnicians() {
        return ResponseEntity.ok(technicianService.getAllTechnicians());
    }

    /**
     * Get technician by ID
     * GET /api/technicians/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TechnicianResponse> getTechnicianById(@PathVariable String id) {
        return ResponseEntity.ok(technicianService.getTechnicianById(id));
    }

    /**
     * Get available technicians (enabled and online)
     * GET /api/technicians/available
     */
    @GetMapping("/available")
    public ResponseEntity<List<TechnicianResponse>> getAvailableTechnicians() {
        return ResponseEntity.ok(technicianService.getAvailableTechnicians());
    }

    /**
     * Get technicians by status
     * GET /api/technicians/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<TechnicianResponse>> getTechniciansByStatus(
            @PathVariable boolean status) {
        return ResponseEntity.ok(technicianService.getTechniciansByStatus(status));
    }

    /**
     * Get technicians ordered by workload
     * GET /api/technicians/workload
     */
    @GetMapping("/workload")
    public ResponseEntity<List<TechnicianWorkloadResponse>> getTechniciansOrderedByWorkload() {
        return ResponseEntity.ok(technicianService.getTechniciansOrderedByWorkload());
    }

    /**
     * Search technicians by name or username
     * GET /api/technicians/search?q={searchTerm}
     */
    @GetMapping("/search")
    public ResponseEntity<List<TechnicianResponse>> searchTechnicians(
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(technicianService.searchTechnicians(q));
    }

    /**
     * Get technician performance metrics
     * GET /api/technicians/{id}/performance
     */
    @GetMapping("/{id}/performance")
    public ResponseEntity<TechnicianPerformanceResponse> getTechnicianPerformance(
            @PathVariable String id) {
        return ResponseEntity.ok(technicianService.getTechnicianPerformance(id));
    }

    /**
     * Get all technicians performance metrics
     * GET /api/technicians/performance/all
     */
    @GetMapping("/performance/all")
    public ResponseEntity<List<TechnicianPerformanceResponse>> getAllTechniciansPerformance() {
        return ResponseEntity.ok(technicianService.getAllTechniciansPerformance());
    }

    /**
     * Get active task count for technician
     * GET /api/technicians/{id}/active-tasks/count
     */
    @GetMapping("/{id}/active-tasks/count")
    public ResponseEntity<Long> getActiveTaskCount(@PathVariable String id) {
        return ResponseEntity.ok(technicianService.getActiveTaskCount(id));
    }

    /**
     * Get completed task count for technician
     * GET /api/technicians/{id}/completed-tasks/count
     */
    @GetMapping("/{id}/completed-tasks/count")
    public ResponseEntity<Long> getCompletedTaskCount(@PathVariable String id) {
        return ResponseEntity.ok(technicianService.getCompletedTaskCount(id));
    }

    /**
     * Create new technician
     * POST /api/technicians
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TechnicianResponse> createTechnician(
            @Valid @RequestBody TechnicianRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        String creatorId = principal.getId();
        return ResponseEntity.ok(technicianService.createTechnician(request, creatorId));
    }

    /**
     * Update technician
     * PUT /api/technicians/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TechnicianResponse> updateTechnician(
            @PathVariable String id,
            @Valid @RequestBody TechnicianUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        String updatorId = principal.getId();
        return ResponseEntity.ok(technicianService.updateTechnician(id, request, updatorId));
    }

    /**
     * Delete technician
     * DELETE /api/technicians/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteTechnician(@PathVariable String id,
                                                            @AuthenticationPrincipal UserDetailsImpl principal) {
        String updatorId = principal.getId();
        technicianService.deleteTechnician(id, updatorId);
        return ResponseEntity.ok(new MessageResponse("Technician deleted successfully"));
    }

    /**
     * Toggle technician status (online/offline)
     * PATCH /api/technicians/{id}/toggle-status
     */
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TechnicianResponse> toggleTechnicianStatus(@PathVariable String id,
                                                                     @AuthenticationPrincipal UserDetailsImpl principal) {
        String updatorId = principal.getId();
        return ResponseEntity.ok(technicianService.toggleTechnicianStatus(id, updatorId));
    }
}
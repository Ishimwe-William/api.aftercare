package com.bunsen.api.aftercare.controller;

import com.bunsen.api.aftercare.dto.request.MotorcycleRequest;
import com.bunsen.api.aftercare.dto.request.MotorcycleStatusUpdateRequest;
import com.bunsen.api.aftercare.dto.response.MessageResponse;
import com.bunsen.api.aftercare.dto.response.MotorcycleResponse;
import com.bunsen.api.aftercare.dto.response.MotorcycleStatisticsResponse;
import com.bunsen.api.aftercare.model.Motorcycle;
import com.bunsen.api.aftercare.service.MotorcycleService;
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

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/motorcycles")
@PreAuthorize("hasRole('ADMIN') or hasRole('TECHNICIAN')")
public class MotorcycleController {

    private final MotorcycleService motorcycleService;

    public MotorcycleController(MotorcycleService motorcycleService) {
        this.motorcycleService = motorcycleService;
    }

    @GetMapping
    public ResponseEntity<Page<MotorcycleResponse>> getAllMotorcycles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(motorcycleService.getAllMotorcycles(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MotorcycleResponse> getMotorcycleById(@PathVariable String id) {
        return ResponseEntity.ok(motorcycleService.getMotorcycleById(id));
    }

    @GetMapping("/qr/{qrCode}")
    public ResponseEntity<MotorcycleResponse> getMotorcycleByQrCode(@PathVariable String qrCode) {
        return ResponseEntity.ok(motorcycleService.getMotorcycleByQrCode(qrCode));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<MotorcycleResponse>> getMotorcyclesByStatus(
            @PathVariable Motorcycle.MotorcycleStatus status) {
        return ResponseEntity.ok(motorcycleService.getMotorcyclesByStatus(status));
    }

    @GetMapping("/owner/phone/{phone}")
    public ResponseEntity<List<MotorcycleResponse>> getMotorcyclesByOwnerPhone(
            @PathVariable String phone) {
        return ResponseEntity.ok(motorcycleService.getMotorcyclesByOwnerPhone(phone));
    }

    @GetMapping("/owner/email/{email}")
    public ResponseEntity<List<MotorcycleResponse>> getMotorcyclesByOwnerEmail(
            @PathVariable String email) {
        return ResponseEntity.ok(motorcycleService.getMotorcyclesByOwnerEmail(email));
    }

    @GetMapping("/in-service")
    public ResponseEntity<Page<MotorcycleResponse>> getMotorcyclesInService(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(motorcycleService.getMotorcyclesInService(pageable));
    }

    @GetMapping("/needing-service")
    public ResponseEntity<Page<MotorcycleResponse>> getMotorcyclesNeedingService(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(motorcycleService.getMotorcyclesNeedingService(pageable));
    }

    @GetMapping("/statistics")
    public ResponseEntity<MotorcycleStatisticsResponse> getMotorcycleStatistics() {
        return ResponseEntity.ok(motorcycleService.getMotorcycleStatistics());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MotorcycleResponse> createMotorcycle(
            @Valid @RequestBody MotorcycleRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        String creatorId = principal.getId();
        MotorcycleResponse response = motorcycleService.createMotorcycle(request, creatorId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MotorcycleResponse> updateMotorcycle(
            @PathVariable String id,
            @Valid @RequestBody MotorcycleRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        String updaterId = principal.getId();
        return ResponseEntity.ok(motorcycleService.updateMotorcycle(id, request, updaterId));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MotorcycleResponse> updateMotorcycleStatus(
            @PathVariable String id,
            @Valid @RequestBody MotorcycleStatusUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        String updaterId = principal.getId();
        return ResponseEntity.ok(motorcycleService.updateMotorcycleStatus(id, request, updaterId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteMotorcycle(@PathVariable String id,
                                                            @AuthenticationPrincipal UserDetailsImpl principal) {
        String deleterId = principal.getId();
        motorcycleService.deleteMotorcycle(id, deleterId);
        return ResponseEntity.ok(new MessageResponse("Motorcycle deleted successfully"));
    }
}
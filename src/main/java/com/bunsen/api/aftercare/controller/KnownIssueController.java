package com.bunsen.api.aftercare.controller;

import com.bunsen.api.aftercare.dto.KnownIssueDTO;
import com.bunsen.api.aftercare.service.KnownIssueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/known-issues")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class KnownIssueController {
    private final KnownIssueService knownIssueService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SUPERVISOR', 'TECHNICIAN')")
    public ResponseEntity<List<KnownIssueDTO.Response>> getAllIssues() {
        return ResponseEntity.ok(knownIssueService.getAllIssues());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<KnownIssueDTO.Response> createIssue(
            @Valid @RequestBody KnownIssueDTO.CreateRequest request) {
        return new ResponseEntity<>(knownIssueService.createIssue(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<KnownIssueDTO.Response> updateIssue(
            @PathVariable Long id,
            @Valid @RequestBody KnownIssueDTO.UpdateRequest request) {
        return ResponseEntity.ok(knownIssueService.updateIssue(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<Void> deleteIssue(@PathVariable Long id) {
        knownIssueService.deleteIssue(id);
        return ResponseEntity.noContent().build();
    }
}

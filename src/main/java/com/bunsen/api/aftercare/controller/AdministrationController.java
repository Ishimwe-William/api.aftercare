package com.bunsen.api.aftercare.controller;

import com.bunsen.api.aftercare.dto.request.UserManagementRequest;
import com.bunsen.api.aftercare.dto.response.MessageResponse;
import com.bunsen.api.aftercare.model.User;
import com.bunsen.api.aftercare.service.AdministrationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdministrationController {

    private final AdministrationService administrationService;

    public AdministrationController(AdministrationService administrationService) {
        this.administrationService = administrationService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(administrationService.getAllUsers());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(administrationService.getUserById(id));
    }

    @PostMapping("/users")
    public ResponseEntity<User> createUser(@Valid @RequestBody UserManagementRequest request) {
        return ResponseEntity.ok(administrationService.createUser(request));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<User> updateUser(@PathVariable String id,
                                         @Valid @RequestBody UserManagementRequest request) {
        return ResponseEntity.ok(administrationService.updateUser(id, request));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable String id) {
        administrationService.deleteUser(id);
        return ResponseEntity.ok(new MessageResponse("User deleted successfully"));
    }

    @PutMapping("/users/{id}/toggle-status")
    public ResponseEntity<User> toggleUserStatus(@PathVariable String id) {
        return ResponseEntity.ok(administrationService.toggleUserStatus(id));
    }
}

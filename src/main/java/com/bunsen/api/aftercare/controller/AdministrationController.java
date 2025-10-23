package com.bunsen.api.aftercare.controller;

import com.bunsen.api.aftercare.dto.request.UserManagementRequest;
import com.bunsen.api.aftercare.dto.response.MessageResponse;
import com.bunsen.api.aftercare.dto.response.UserResponse;
import com.bunsen.api.aftercare.enums.ERole;
import com.bunsen.api.aftercare.exception.ValidationException;
import com.bunsen.api.aftercare.service.AdministrationService;
import com.bunsen.api.aftercare.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    // GET /users: Returns List<UserResponse> (Safe)
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(administrationService.getAllUsers());
    }

    // GET /users/{id}: Returns UserResponse (Safe)
    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(administrationService.getUserById(id));
    }

    // POST /users: Returns UserResponse (Safe) and uses 201 Created
    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserManagementRequest request,
                                                   @AuthenticationPrincipal UserDetailsImpl principal) {
        // Pass the acting user's ID to the service
        String creatorId = principal.getId();

        UserResponse user = administrationService.createUser(request, creatorId);
        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }

    // PUT /users/{id}: Returns UserResponse (Safe)
    @PutMapping("/users/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable String id,
                                                   @Valid @RequestBody UserManagementRequest request,
                                                   @AuthenticationPrincipal UserDetailsImpl principal) {
        String updaterId = principal.getId();
        return ResponseEntity.ok(administrationService.updateUser(id, request, updaterId));
    }

    // DELETE /users/{id}: Remains fine, returns 200 OK MessageResponse.
    @DeleteMapping("/users/{id}")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable String id,
                                                      @AuthenticationPrincipal UserDetailsImpl principal) {
        String deleterId = principal.getId();
        administrationService.deleteUser(id, deleterId);
        return ResponseEntity.ok(new MessageResponse("User deleted successfully"));
    }

    // PUT /users/{id}/toggle-status: Returns UserResponse (Safe)
    @PutMapping("/users/{id}/toggle-status")
// CHANGED: Return type from User to UserResponse.
    public ResponseEntity<UserResponse> toggleUserStatus(@PathVariable String id,
                                                         @AuthenticationPrincipal UserDetailsImpl principal) {
        String updater = principal.getId();
        // The service layer method should now return UserResponse.
        return ResponseEntity.ok(administrationService.toggleUserStatus(id, updater));
    }

    // GET /users/role/{role}: Returns List<UserResponse> (Safe)
    @GetMapping("/users/role/{role}")
// CHANGED: Return type from List<User> to List<UserResponse>.
    public ResponseEntity<List<UserResponse>> getUsersByRole(@PathVariable String role) {
        ERole eRole;
        try {
            eRole = ERole.valueOf("ROLE_" + role.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Throw ValidationException for invalid input (Modular Exception Handling).
            throw new ValidationException("Invalid role: " + role);
        }
        // The service layer method should now return List<UserResponse>.
        return ResponseEntity.ok(administrationService.getUsersByRole(eRole));
    }
}
package com.bunsen.api.aftercare.controller;

import com.bunsen.api.aftercare.dto.request.UserManagementRequest;
import com.bunsen.api.aftercare.dto.response.UserResponse;
import com.bunsen.api.aftercare.service.AuthService;
import com.bunsen.api.aftercare.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/profile")
@PreAuthorize("isAuthenticated()")
public class ProfileController {

    private final AuthService userService;

    public ProfileController(AuthService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal UserDetailsImpl principal) {
        String userId = principal.getId();
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @PutMapping
    public ResponseEntity<UserResponse> updateProfile(
            @Valid @RequestBody UserManagementRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        String userId = principal.getId();
        // Implement update logic in service (e.g., update name, email, etc.; handle password separately if needed)
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }
}
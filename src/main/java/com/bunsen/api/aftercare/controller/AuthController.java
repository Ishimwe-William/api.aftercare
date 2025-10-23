package com.bunsen.api.aftercare.controller;

import com.bunsen.api.aftercare.dto.request.ForgotPasswordRequest;
import com.bunsen.api.aftercare.dto.request.LoginRequest;
import com.bunsen.api.aftercare.dto.request.ResetPasswordRequest;
import com.bunsen.api.aftercare.dto.request.SignupRequest;
import com.bunsen.api.aftercare.dto.response.JwtResponse;
import com.bunsen.api.aftercare.dto.response.MessageResponse;
import com.bunsen.api.aftercare.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(jwtResponse); // HTTP 200 OK
    }

    @PostMapping({"/register"})
    public ResponseEntity<MessageResponse> registerUser(@Valid @RequestBody SignupRequest signupRequest) {
        MessageResponse messageResponse = authService.registerUser(signupRequest);
        return ResponseEntity.ok(messageResponse); // HTTP 200 OK
    }

    @PostMapping("/verify-email")
    public ResponseEntity<JwtResponse> verifyEmail(@RequestParam("token") String token) {
        JwtResponse jwtResponse = authService.verifyEmail(token);
        return ResponseEntity.ok(jwtResponse); // HTTP 200 OK
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        MessageResponse response = authService.sendPasswordResetToken(request.getEmail());
        // Return 200 OK even if email is not found to prevent user enumeration
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        MessageResponse response = authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(response); // HTTP 200 OK
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            // Retaining explicit validation for required input fields (DRY violation is minor here)
            return ResponseEntity.badRequest().body(new MessageResponse("Refresh token is required"));
        }

        JwtResponse jwtResponse = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(jwtResponse); // HTTP 200 OK
    }

    @PostMapping("/google")
    public ResponseEntity<?> authenticateWithGoogle(@RequestBody Map<String, String> payload) {
        String idToken = payload.get("idToken");

        if (idToken == null || idToken.trim().isEmpty()) {
            // Retaining explicit validation
            return ResponseEntity.badRequest().body(new MessageResponse("Google ID token is required"));
        }

        JwtResponse jwtResponse = authService.authenticateWithGoogle(idToken);
        return ResponseEntity.ok(jwtResponse); // HTTP 200 OK
    }

    @PostMapping("/web/google")
    public ResponseEntity<?> authenticateWebWithGoogle(@RequestBody Map<String, String> payload) {
        String code = payload.get("code");

        if (code == null || code.trim().isEmpty()) {
            // Retaining explicit validation
            return ResponseEntity.badRequest().body(new MessageResponse("Authorization code is required"));
        }

        JwtResponse jwtResponse = authService.authenticateWebWithGoogle(code);
        return ResponseEntity.ok(jwtResponse); // HTTP 200 OK
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Retaining explicit validation
            return ResponseEntity.badRequest().body(new MessageResponse("Invalid authorization header"));
        }

        String token = authHeader.substring(7);
        authService.logout(token);
        return ResponseEntity.ok(new MessageResponse("Logged out successfully")); // HTTP 200 OK
    }
}
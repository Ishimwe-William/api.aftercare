package com.bunsen.api.aftercare.controller;

import com.bunsen.api.aftercare.dto.request.ForgotPasswordRequest;
import com.bunsen.api.aftercare.dto.request.LoginRequest;
import com.bunsen.api.aftercare.dto.request.ResetPasswordRequest;
import com.bunsen.api.aftercare.dto.request.SignupRequest;
import com.bunsen.api.aftercare.dto.response.JwtResponse;
import com.bunsen.api.aftercare.dto.response.MessageResponse;
import com.bunsen.api.aftercare.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
            return ResponseEntity.ok(jwtResponse);
        } catch (Exception e) {
            logger.error("Login failed for user: {}", loginRequest.getUsernameOrEmail(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Invalid credentials"));
        }
    }

    @PostMapping({"/register"})
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signupRequest) {
        try {
            MessageResponse messageResponse = authService.registerUser(signupRequest);
            return ResponseEntity.ok(messageResponse);
        } catch (Exception e) {
            logger.error("Registration failed for user: {}", signupRequest.getUsername(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Registration failed"));
        }
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam("token") String token) {
        try {
            JwtResponse jwtResponse = authService.verifyEmail(token);
            return ResponseEntity.ok(jwtResponse);
        } catch (Exception e) {
            logger.error("Email verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Email verification failed"));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            MessageResponse response = authService.sendPasswordResetToken(request.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Forgot password failed for email: {}", request.getEmail(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Failed to send reset password email"));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            MessageResponse response = authService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Reset password failed for token: {}", request.getToken(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Failed to reset password"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Refresh token is required"));
        }

        try {
            JwtResponse jwtResponse = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(jwtResponse);
        } catch (Exception e) {
            logger.error("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Token refresh failed"));
        }
    }

    @PostMapping("/google")
    public ResponseEntity<?> authenticateWithGoogle(@RequestBody Map<String, String> payload) {
        String idToken = payload.get("idToken");

        if (idToken == null || idToken.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Google ID token is required"));
        }

        try {
            JwtResponse jwtResponse = authService.authenticateWithGoogle(idToken);
            return ResponseEntity.ok(jwtResponse);
        } catch (Exception e) {
            logger.error("Google authentication failed. Exception type: {}, Message: {}, Cause: {}",
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    e.getCause() != null ? e.getCause().getMessage() : "No cause",
                    e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Google authentication failed"));
        }
    }
}
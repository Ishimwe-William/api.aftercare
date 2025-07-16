package com.bunsen.api.aftercare.service;

import com.bunsen.api.aftercare.dto.request.LoginRequest;
import com.bunsen.api.aftercare.dto.request.SignupRequest;
import com.bunsen.api.aftercare.dto.response.JwtResponse;
import com.bunsen.api.aftercare.dto.response.MessageResponse;
import com.bunsen.api.aftercare.enums.ERole;
import com.bunsen.api.aftercare.exception.BadRequestException;
import com.bunsen.api.aftercare.model.Role;
import com.bunsen.api.aftercare.model.User;
import com.bunsen.api.aftercare.repository.RoleRepository;
import com.bunsen.api.aftercare.repository.UserRepository;
import com.bunsen.api.aftercare.security.jwt.JwtUtils;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;
    private final EmailService emailService;

    @Value("${email.sender.resetPasswordUrl}")
    private String resetPasswordUrlBase;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository,
                       RoleRepository roleRepository, PasswordEncoder encoder, JwtUtils jwtUtils, EmailService emailService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
        this.emailService = emailService;
    }

    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        logger.debug("Attempting to authenticate user with username or email: {}", loginRequest.getUsernameOrEmail());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsernameOrEmail(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            logger.debug("User authenticated successfully: {}", userDetails.getUsername());
            return new JwtResponse(jwt, userDetails.getId(), userDetails.getUsername(), userDetails.getEmail(), roles);
        } catch (Exception e) {
            logger.error("Authentication error for input {}: {}", loginRequest.getUsernameOrEmail(), e.getMessage());
            throw new BadRequestException("Invalid username or password");
        }
    }

    @Transactional
    public MessageResponse registerUser(SignupRequest signupRequest) {
        logger.debug("Attempting to register new user: {}", signupRequest.getUsername());

        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            logger.debug("Registration failed: Username {} is already taken", signupRequest.getUsername());
            return new MessageResponse("Error: Username is already taken!");
        }

        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            logger.debug("Registration failed: Email {} is already in use", signupRequest.getEmail());
            return new MessageResponse("Error: Email is already in use!");
        }

        // Create a new user's account
        User user = new User();
        user.setUsername(signupRequest.getUsername());
        user.setEmail(signupRequest.getEmail());
        user.setPassword(encoder.encode(signupRequest.getPassword()));
        user.setFullName(signupRequest.getFullName());
        user.setPhoneNumber(signupRequest.getPhoneNumber());

        Set<String> strRoles = signupRequest.getRoles();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null || strRoles.isEmpty()) {
            logger.debug("Setting default CUSTOMER role for user: {}", signupRequest.getUsername());
            Role userRole = roleRepository.findByName(ERole.ROLE_CUSTOMER)
                    .orElseThrow(() -> {
                        logger.error("Error: ROLE_CUSTOMER not found in database");
                        return new RuntimeException("Error: Role CUSTOMER is not found in database.");
                    });
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin" -> {
                        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(adminRole);
                    }
                    case "staff" -> {
                        Role modRole = roleRepository.findByName(ERole.ROLE_STAFF)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(modRole);
                    }
                    default -> {
                        Role userRole = roleRepository.findByName(ERole.ROLE_CUSTOMER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(userRole);
                    }
                }
            });
        }

        user.setRoles(roles);
        userRepository.save(user);
        logger.info("User registered successfully: {}", signupRequest.getUsername());

        return new MessageResponse("User registered successfully!");
    }

    public MessageResponse sendPasswordResetToken(String email) {
        logger.debug("Generating password reset token for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Error: Email not found"));

        String token = jwtUtils.generatePasswordResetToken(user.getUsername());

        String subject = "Password Reset Token";
        String resetPasswordUrl = resetPasswordUrlBase + "?token=" + token;
        String body = "Your password reset token is: \n\n"
                + token
                + "\n\nFollow this link to reset your password: \n\n"
                + "<a href='" + resetPasswordUrl + "'>Reset Password</a>";

        try {
            emailService.sendEmail(email, "Aftercare App", subject, body);
            logger.info("Password reset token sent to {}", email);
            return new MessageResponse("Password reset token sent to your email.");
        } catch (MessagingException e) {
            logger.error("Error sending password reset token to {}: {}", email, e.getMessage());
            throw new RuntimeException("Error sending password reset token");
        }
    }

    public MessageResponse resetPassword(String token, String newPassword) {
        logger.debug("Attempting to reset password using token");

        if (!jwtUtils.validateJwtToken(token)) {
            logger.warn("Invalid or expired token used for password reset");
            return new MessageResponse("Invalid or expired token.");
        }

        String username = jwtUtils.getUserNameFromJwtToken(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(encoder.encode(newPassword));
        userRepository.save(user);

        logger.info("Password reset successful for user: {}", username);
        return new MessageResponse("Password reset successfully.");
    }
}
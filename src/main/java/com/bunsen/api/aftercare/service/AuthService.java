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
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
    private final PasswordEncoder passwordEncoder;
    private final GoogleTokenService googleTokenService;

    @Value("${email.sender.resetPasswordUrl}")
    private String resetPasswordUrlBase;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository,
                       RoleRepository roleRepository, PasswordEncoder encoder, JwtUtils jwtUtils,
                       EmailService emailService, PasswordEncoder passwordEncoder, GoogleTokenService googleTokenService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.googleTokenService = googleTokenService;
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
            String refreshToken = jwtUtils.generateRefreshToken(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            logger.debug("User authenticated successfully: {}", userDetails.getUsername());
            return new JwtResponse(jwt, refreshToken, userDetails.getId(),userDetails.getUser().getFullName(), userDetails.getUsername(), userDetails.getEmail(), roles, userDetails.getUser().getPhoneNumber(), userDetails.getUser().getPhotoUrl());
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

    public JwtResponse authenticateWithGoogle(String idToken) {
        logger.debug("Attempting to authenticate user with Google token: {}", idToken);
        try {
            GoogleIdToken token = googleTokenService.verifyToken(idToken);
            GoogleTokenService.GoogleUserInfo userInfo = googleTokenService.getUserInfo(token);

            if (!userInfo.isEmailVerified()) {
                throw new RuntimeException("Email must be verified");
            }

            User user = userRepository.findByEmail(userInfo.getEmail())
                    .orElseGet(() -> createUserFromGoogleInfo(userInfo));

            logger.debug("User created or found: {}", user.getUsername());

            return generateJwtForUser(user);
        } catch (Exception e) {
            logger.error("Google authentication failed: {}", e.getMessage());
            throw new RuntimeException("Invalid Google token");
        }
    }

    private User createUserFromGoogleInfo(GoogleTokenService.GoogleUserInfo userInfo) {
        User user = new User();
        user.setEmail(userInfo.getEmail());
        user.setUsername(generateUniqueUsername(userInfo.getEmail().split("@")[0]));
        user.setFullName(userInfo.getName() != null ? userInfo.getName() : "Google User");
        user.setPhotoUrl(userInfo.getPictureUrl());
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setRoles(Set.of(roleRepository.findByName(ERole.ROLE_CUSTOMER).orElseThrow()));

        return userRepository.save(user);
    }

    private JwtResponse generateJwtForUser(User user) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());

        UserDetailsImpl userDetails = new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                authorities,
                user
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        return getJwtResponse(user, authentication);
    }

    private JwtResponse getJwtResponse(User user, Authentication authentication) {
        String jwt = jwtUtils.generateJwtToken(authentication);
        String refreshToken = jwtUtils.generateRefreshToken(authentication);

        List<String> roles = user.getRoles().stream().map(role -> role.getName().name()).collect(Collectors.toList());
        return new JwtResponse(jwt, refreshToken, user.getId(),user.getFullName(), user.getUsername(), user.getEmail(), roles, user.getPhoneNumber(), user.getPhotoUrl());
    }

    private String generateUniqueUsername(String baseUsername) {
        String username = baseUsername;
        int counter = 1;

        while (userRepository.findByUsername(username).isPresent()) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }

    public JwtResponse refreshToken(String refreshToken) {
        if (!jwtUtils.validateJwtToken(refreshToken)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        String username = jwtUtils.getUserNameFromJwtToken(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());

        UserDetailsImpl userDetails = new UserDetailsImpl(
                user.getId(), user.getUsername(), user.getEmail(), user.getPassword(),
                authorities, user);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, authorities);

        return getJwtResponse(user, authentication);
    }
}
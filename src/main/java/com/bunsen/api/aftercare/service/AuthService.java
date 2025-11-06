package com.bunsen.api.aftercare.service;

import com.bunsen.api.aftercare.dto.request.LoginRequest;
import com.bunsen.api.aftercare.dto.request.SignupRequest;
import com.bunsen.api.aftercare.dto.request.UserManagementRequest;
import com.bunsen.api.aftercare.dto.response.JwtResponse;
import com.bunsen.api.aftercare.dto.response.MessageResponse;
import com.bunsen.api.aftercare.dto.response.UserResponse;
import com.bunsen.api.aftercare.enums.ERole;
import com.bunsen.api.aftercare.exception.BadRequestException;
import com.bunsen.api.aftercare.exception.ResourceNotFoundException;
import com.bunsen.api.aftercare.model.Role;
import com.bunsen.api.aftercare.model.User;
import com.bunsen.api.aftercare.repository.RoleRepository;
import com.bunsen.api.aftercare.repository.UserRepository;
import com.bunsen.api.aftercare.security.jwt.JwtUtils;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.json.JSONObject;
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
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.security.SecureRandom;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
    private final DatabaseJwtBlacklistService databaseJwtBlacklistService;

    @Value("${email.sender.baseUrl}")
    private String baseUrl;

    @Value("${email.sender.resetPasswordUrl}")
    private String resetPasswordUrlBase;

    @Value("${google.clientId}")
    private String googleClientId;

    @Value("${google.clientSecret}")
    private String googleClientSecret;

    @Value("${google.redirectUri}")
    private String googleRedirectUri;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository,
                       RoleRepository roleRepository, PasswordEncoder encoder, JwtUtils jwtUtils,
                       EmailService emailService, PasswordEncoder passwordEncoder, GoogleTokenService googleTokenService, DatabaseJwtBlacklistService databaseJwtBlacklistService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.googleTokenService = googleTokenService;
        this.databaseJwtBlacklistService = databaseJwtBlacklistService;
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

            // Get user details and check status
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            User user = userDetails.getUser();

            // Check if user is active (status should be true)
            if (!user.isStatus()) {
                logger.warn("Authentication attempt by inactive user: {}", userDetails.getUsername());
                throw new BadRequestException("Account is inactive. Please contact support.");
            }

            String jwt = jwtUtils.generateJwtToken(authentication);
            String refreshToken = jwtUtils.generateRefreshToken(authentication);

            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            logger.debug("User authenticated successfully: {}", userDetails.getUsername());
            return new JwtResponse(
                    jwt,
                    refreshToken,
                    userDetails.getId(),
                    userDetails.getUser().getFullName(),
                    userDetails.getUsername(),
                    userDetails.getEmail(),
                    roles,
                    userDetails.getUser().getPhoneNumber(),
                    userDetails.getUser().getPhotoUrl(),
                    userDetails.getUser().getUpdatedAt(),
                    userDetails.getUser().isPasswordChangeRequired(),
                    userDetails.getUser().isEnabled(),
                    userDetails.getUser().isStatus()
            );
        } catch (Exception e) {
            logger.error("Authentication error for input {}: {}", loginRequest.getUsernameOrEmail(), e.getMessage());
            throw new BadRequestException("Invalid username or password");
        }
    }

    @Transactional
    public MessageResponse registerUser(SignupRequest signupRequest) {
        logger.debug("Registering new user with input: {}", signupRequest);
        logger.debug("Attempting to register new user: {}", signupRequest.getEmail());

        Optional<User> existingUser = userRepository.findByEmailIgnoreCase(signupRequest.getEmail());

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (!user.isEnabled()) {
                // User exists but not verified, send new verification email
                String verificationToken = jwtUtils.generateEmailVerificationToken(user.getUsername());

                String body = "Your email verification token is: \n\n"
                        + verificationToken
                        + "\n\nFollow this to verify your email address: \n\n"
                        + "<a href='" + baseUrl + "auth/verify-email?token=" + verificationToken + "'>Verify Email</a>";

                try {
                    emailService.sendEmail(user.getEmail(), "Aftercare App", "Verify your email address", body);
                    logger.info("New verification email sent to {}", user.getEmail());
                    return new MessageResponse("A new verification email has been sent to your email address.");
                } catch (MessagingException e) {
                    logger.error("Error sending verification email to {}: {}", user.getEmail(), e.getMessage());
                    throw new RuntimeException("Error sending verification email");
                }
            }

            logger.debug("Registration failed: Email {} is already in use", signupRequest.getEmail());
            throw new BadRequestException("Error: Email is already in use!");
        }

        // Rest of the existing registration logic...
        String baseUsername = (signupRequest.getFirstName() + signupRequest.getLastName()).toLowerCase();
        String username = baseUsername;
        int counter = 1;
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(signupRequest.getEmail());
        user.setPhoneNumber(signupRequest.getPhoneNumber());
        user.setPassword(encoder.encode(signupRequest.getPassword()));
        user.setFullName(signupRequest.getFirstName() + " " + signupRequest.getLastName());
        user.setEnabled(false);
        user.setPasswordChangeRequired(false);
        user.setStatus(true);

        Set<String> strRoles = signupRequest.getRoles();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null || strRoles.isEmpty()) {
            logger.debug("Setting default CUSTOMER role for user: {}", username);
            Role userRole = roleRepository.findByName(ERole.ROLE_TECHNICIAN)
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
                        Role userRole = roleRepository.findByName(ERole.ROLE_TECHNICIAN)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(userRole);
                    }
                }
            });
        }

        user.setRoles(roles);
        userRepository.save(user);
        logger.info("User registered successfully: {}", username);

        String verificationToken = jwtUtils.generateEmailVerificationToken(user.getUsername());

        String body = "Your email verification token is: \n\n"
                + verificationToken
                + "\n\nFollow this to verify your email address: \n\n"
                + "<a href='" + baseUrl + "auth/verify-email?token=" + verificationToken + "'>Verify Email</a>";

        try {
            emailService.sendEmail(user.getEmail(), "Aftercare App", "Verify your email address", body);
            logger.info("Verification email sent to {}", user.getEmail());
        } catch (MessagingException e) {
            logger.error("Error sending verification email to {}: {}", user.getEmail(), e.getMessage());
            throw new RuntimeException("Error sending verification email");
        }

        return new MessageResponse("User registered successfully. Please check your email to verify your account.");
    }

    public JwtResponse verifyEmail(String token) {
        if (!jwtUtils.validateJwtToken(token)) {
            logger.warn("Invalid or expired token used for email verification");
            throw new BadRequestException("Invalid or expired token.");
        }

        String purpose = jwtUtils.getPurposeFromJwtToken(token);
        if (!"email_verification".equals(purpose)) {
            logger.warn("Token purpose mismatch for email verification");
            throw new BadRequestException("Invalid token purpose.");
        }

        String username = jwtUtils.getUserNameFromJwtToken(token);
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("User","Username",username));

        if (user.isEnabled()) {
            logger.info("Email already verified for user: {}", username);
            return generateJwtForUser(user);
        }

        user.setEnabled(true);
        userRepository.save(user);

        logger.info("Email verified successfully for user: {}", username);
        return generateJwtForUser(user);
    }

    public MessageResponse sendPasswordResetToken(String email) {
        logger.debug("Generating password reset token for email: {}", email);

        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(email);
        if (userOptional.isEmpty()) {
            logger.info("Password reset requested for non-existent email: {}", email);
            return new MessageResponse("If the email is registered, a reset link will be sent.");
        }

        User user = userOptional.get();
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
        } catch (MessagingException e) {
            logger.error("Error sending password reset token to {}: {}", email, e.getMessage());
            throw new RuntimeException("Error sending password reset token");
        }

        return new MessageResponse("If the email is registered, a reset link will be sent.");
    }

    public MessageResponse resetPassword(String token, String newPassword) {
        logger.debug("Attempting to reset password using token");

        if (!jwtUtils.validateJwtToken(token)) {
            logger.warn("Invalid or expired token used for password reset");
            return new MessageResponse("Invalid or expired token.");
        }

        String purpose = jwtUtils.getPurposeFromJwtToken(token);
        if (!"password_reset".equals(purpose)) {
            logger.warn("Token purpose mismatch for password reset");
            return new MessageResponse("Invalid token purpose.");
        }

        String username = jwtUtils.getUserNameFromJwtToken(token);
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(encoder.encode(newPassword));
        userRepository.save(user);

        logger.info("Password reset successful for user: {}", username);
        return new MessageResponse("Password reset successfully.");
    }

    public JwtResponse refreshToken(String refreshToken) {
        if (!jwtUtils.validateJwtToken(refreshToken)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        String username = jwtUtils.getUserNameFromJwtToken(refreshToken);
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isEnabled()) {
            throw new RuntimeException("User account is disabled");
        }

        // Add status check
        if (!user.isStatus()) {
            throw new RuntimeException("User account is inactive");
        }

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

    @Transactional
    public JwtResponse authenticateWithGoogle(String idToken) {
        try {
            GoogleIdToken token = googleTokenService.verifyToken(idToken);
            GoogleTokenService.GoogleUserInfo userInfo = googleTokenService.getUserInfo(token);

            String email = userInfo.getEmail();
            String name = userInfo.getName();

            Optional<User> existingUser = userRepository.findByEmailIgnoreCase(email);

            if (existingUser.isPresent()) {
                User user = existingUser.get();

                if (!user.isStatus()) {
                    logger.warn("Google authentication attempt by inactive user: {}", user.getUsername());
                    throw new BadRequestException("Account is inactive. Please contact support.");
                }

                getGoogleUserInfo(userInfo, name, user);
                return generateJwtForUser(user);
            }

            // NEW USER - Create but don't save yet
            User user = new User();
            user.setEmail(userInfo.getEmail());
            user.setFullName(userInfo.getName() != null ? userInfo.getName() : "Google User");
            user.setPhotoUrl(userInfo.getPictureUrl());
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setEnabled(true);
            user.setStatus(true);
            user.setPasswordChangeRequired(false);

            // Generate username BEFORE saving
            createNewUser(email, user);

            Role userRole = roleRepository.findByName(ERole.ROLE_STAFF)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            user.setRoles(Set.of(userRole));

            userRepository.save(user);  // Save once with correct username
            return generateJwtForUser(user);

        } catch (Exception e) {
            logger.error("Google authentication failed: {}", e.getMessage());
            throw new RuntimeException("Invalid Google token", e);
        }
    }

    private void getGoogleUserInfo(GoogleTokenService.GoogleUserInfo userInfo, String name, User user) {
        if (user.getPhotoUrl() == null && userInfo.getPictureUrl() != null) {
            user.setPhotoUrl(userInfo.getPictureUrl());
        }
        if (!user.getFullName().equalsIgnoreCase(name)) {
            user.setFullName(name);
        }
        if (!user.isEnabled()) {
            user.setEnabled(true);
        }
        userRepository.save(user);
    }

    private void createNewUser(String email, User user) {
        String baseUsername = email.split("@")[0];
        String username = baseUsername.length() > 20 ? baseUsername.substring(0, 20) : baseUsername;

        int counter = 1;
        while (userRepository.findByUsernameIgnoreCase(username).isPresent()) {
            String suffix = counter > 9 ? String.valueOf(counter) : "0" + counter;
            username = (baseUsername.length() > 18 ? baseUsername.substring(0, 18) : baseUsername) + suffix;
            counter++;
        }

        user.setUsername(username);
    }

    @Transactional
    public JwtResponse authenticateWebWithGoogle(String code) {
        try {
            // Step 1: Exchange the authorization code for tokens
            String tokenUrl = "https://oauth2.googleapis.com/token";
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("code", code);
            params.add("client_id", googleClientId);
            params.add("client_secret", googleClientSecret);
            params.add("redirect_uri", googleRedirectUri);
            params.add("grant_type", "authorization_code");

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<String> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to exchange code for tokens: " + response.getStatusCode());
            }

            // Step 2: Extract the ID token from the response
            JSONObject jsonResponse = new JSONObject(response.getBody());
            String idToken = jsonResponse.getString("id_token");

            // Step 3: Verify the ID token and get user information
            GoogleIdToken googleIdToken = googleTokenService.verifyToken(idToken);
            GoogleIdToken.Payload payload = googleIdToken.getPayload();
            GoogleTokenService.GoogleUserInfo userInfo = googleTokenService.getUserInfo(googleIdToken);

            // Step 4: Authenticate or create the user
            String email = userInfo.getEmail();
            String name = (String) payload.get("name");

            Optional<User> existingUser = userRepository.findByEmailIgnoreCase(email);
            User user;

            if (existingUser.isPresent()) {
                user = existingUser.get();

                // Check if user is active (status should be true)
                if (!user.isStatus()) {
                    logger.warn("Web Google authentication attempt by inactive user: {}", user.getUsername());
                    throw new BadRequestException("Account is inactive. Please contact support.");
                }

                // Update user info if necessary
                getGoogleUserInfo(userInfo, name, user);
            } else {
                user = new User();
                user.setEmail(email);
                user.setFullName(name);
                user.setPhotoUrl(userInfo.getPictureUrl());
                user.setEnabled(true);
                // Set status to true for new users (they are active by default)
                user.setStatus(true);

                // Generate a unique username within the size limit (0-20 characters)
                createNewUser(email, user);
                // Assign default role
                Role userRole = roleRepository.findByName(ERole.ROLE_TECHNICIAN)
                        .orElseThrow(() -> new RuntimeException("Role not found"));
                user.setRoles(Set.of(userRole));
                user.setPasswordChangeRequired(false);
                // Set a random password
                user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                userRepository.save(user);
            }

            // Step 5: Generate and return JWT response
            return generateJwtForUser(user);

        } catch (Exception e) {
            logger.error("Web Google authentication failed: {}", e.getMessage(), e);
            throw new RuntimeException("Web Google authentication failed", e);
        }
    }

    private JwtResponse generateJwtForUser(User user) {
        // Check if user is active before generating JWT
        if (!user.isStatus()) {
            logger.warn("JWT generation attempt for inactive user: {}", user.getUsername());
            throw new BadRequestException("Account is inactive. Please contact support.");
        }

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
        return new JwtResponse(jwt, refreshToken, user.getId(), user.getFullName(), user.getUsername(), user.getEmail(), roles, user.getPhoneNumber(), user.getPhotoUrl(), user.getUpdatedAt(), user.isPasswordChangeRequired(), user.isEnabled(), user.isStatus());
    }

    private String generateRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            sb.append(characters.charAt(index));
        }
        return sb.toString();
    }

    public void logout(String token) {
        logger.debug("Processing logout request");

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (!jwtUtils.validateJwtToken(token)) {
            logger.warn("Invalid token used for logout attempt");
            throw new BadRequestException("Invalid token");
        }

        databaseJwtBlacklistService.blacklistToken(token);
        logger.info("User logged out successfully");
    }

    public UserResponse getUserById(String userId) {
        logger.debug("Fetching user profile for ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        return getUserResponse(user);
    }

    private UserResponse getUserResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .toList();

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .photoUrl(user.getPhotoUrl())
                .enabled(user.isEnabled())
                .status(user.isStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }


    @Transactional
    public UserResponse updateProfile(String userId, @Valid UserManagementRequest request) {
        logger.debug("Updating profile for user ID: {} with request: {}", userId, request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Update basic profile information
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }

        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getPhotoUrl() != null) {
            user.setPhotoUrl(request.getPhotoUrl());
        }

        // Save the updated user
        user = userRepository.save(user);
        logger.info("Profile updated successfully for user: {}", user.getUsername());

        // Return the updated user response
        return getUserResponse(user);
    }
}
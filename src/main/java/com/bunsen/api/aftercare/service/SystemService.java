package com.bunsen.api.aftercare.service;

import com.bunsen.api.aftercare.enums.ERole;
import com.bunsen.api.aftercare.model.Role;
import com.bunsen.api.aftercare.model.User;
import com.bunsen.api.aftercare.repository.RoleRepository;
import com.bunsen.api.aftercare.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SystemService {

    private static final String SYSTEM_USERNAME = "SYSTEM_USER";
    private static final String SYSTEM_EMAIL = "system@bunsen.com";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private User systemUser;

    /**
     * Ensures the SYSTEM_USER exists in the database on application startup.
     */
    @PostConstruct
    public void initializeSystemUser() {
        Optional<User> existingUser = userRepository.findByUsernameIgnoreCase(SYSTEM_USERNAME);

        if (existingUser.isEmpty()) {
            User newUser = createSystemUser();
            this.systemUser = newUser;
        } else {
            this.systemUser = existingUser.get();
        }
    }

    private User createSystemUser() {
        User user = new User();
        user.setUsername(SYSTEM_USERNAME);
        user.setEmail(SYSTEM_EMAIL);
        user.setFullName("System Administrator");
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setEnabled(true);
        user.setStatus(true);
        user.setPasswordChangeRequired(false);
        user.setCreatedBy("BOOTSTRAP");

        // Assign a role that ensures no access, or a simple READ_ONLY role
        Role defaultRole = roleRepository.findByName(ERole.ROLE_STAFF)
                .orElse(null); // Assuming ROLE_STAFF exists, otherwise handle it.

        Set<Role> roles = defaultRole != null ? Set.of(defaultRole) : Collections.emptySet();
        user.setRoles(roles);

        return userRepository.save(user);
    }

    /**
     * Gets the SYSTEM_USER entity.
     * @return The immutable SYSTEM_USER.
     */
    public User getSystemUser() {
        if (this.systemUser == null) {
            // Fallback to database lookup if @PostConstruct failed or object was cleared
            this.systemUser = userRepository.findByUsernameIgnoreCase(SYSTEM_USERNAME)
                    .orElseThrow(() -> new RuntimeException("CRITICAL: SYSTEM_USER not found. Database integrity may be compromised."));
        }
        return this.systemUser;
    }

    /**
     * Checks if the user is the special SYSTEM_USER.
     */
    public boolean isSystemUser(String userId) {
        return getSystemUser().getId().equals(userId);
    }
}
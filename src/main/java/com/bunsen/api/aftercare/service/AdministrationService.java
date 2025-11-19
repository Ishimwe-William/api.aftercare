package com.bunsen.api.aftercare.service;

import com.bunsen.api.aftercare.dto.request.UserManagementRequest;
import com.bunsen.api.aftercare.dto.response.UserResponse;
import com.bunsen.api.aftercare.enums.ERole;
import com.bunsen.api.aftercare.exception.DuplicateResourceException;
import com.bunsen.api.aftercare.exception.ResourceNotFoundException;
import com.bunsen.api.aftercare.exception.ValidationException;
import com.bunsen.api.aftercare.model.Role;
import com.bunsen.api.aftercare.model.User;
import com.bunsen.api.aftercare.repository.RoleRepository;
import com.bunsen.api.aftercare.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdministrationService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogService activityLogService;
    private final SystemService systemService;

    public AdministrationService(UserRepository userRepository,
                                 RoleRepository roleRepository,
                                 PasswordEncoder passwordEncoder,
                                 ActivityLogService activityLogService,
                                 SystemService systemService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.activityLogService = activityLogService;
        this.systemService = systemService;
    }

    // UPDATED: Return safe DTO
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // UPDATED: Return safe DTO
    public UserResponse getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return mapToResponse(user);
    }

    // UPDATED: Return safe DTO
    public UserResponse updateUser(String id, UserManagementRequest request, String updaterId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        // Validation check for username/email uniqueness upon update
        if (!user.getUsername().equals(request.getUsername()) && userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setStatus(request.isStatus());
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());

        Set<Role> roles = new HashSet<>();
        request.getRoles().forEach(roleName -> {
            try {
                Role role = roleRepository.findByName(ERole.valueOf(roleName))
                        .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));
                roles.add(role);
            } catch (IllegalArgumentException e) {
                // Use ValidationException for invalid input like role name
                throw new ValidationException("Invalid role name specified: " + roleName);
            }
        });
        user.setRoles(roles);

        User updatedUser = userRepository.save(user);
        activityLogService.createLog(updaterId, "USER_UPDATED",
                String.format("User %s (%s) updated by admin.", updatedUser.getUsername(), updatedUser.getId()));

        return mapToResponse(updatedUser);
    }

    public void deleteUser(String id, String deleterId) {
        // CRITICAL SECURITY CHECK 1: Prevent self-deletion
        if (id.equals(deleterId)) {
            throw new ValidationException("Cannot delete your own user account.");
        }

        // CRITICAL SECURITY CHECK 2: Prevent deletion of the SYSTEM user
        if (systemService.isSystemUser(id)) {
            throw new ValidationException("The SYSTEM user account cannot be deleted.");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        // Dependency Resolution: Reassign Logs
        User systemUser = systemService.getSystemUser();

        // Reassign all logs from the user being deleted to the SYSTEM_USER
        int reassignCount = activityLogService.reassignLogs(user.getId(), systemUser.getId());

        userRepository.delete(user);

        // Log the number of reassigned records.
        activityLogService.createLog(deleterId, "USER_DELETED",
                String.format("User %s (%s) deleted by admin. Logs reassigned: %d.",
                        user.getUsername(), user.getId(), reassignCount));
    }

    // UPDATED: Return safe DTO
    public UserResponse toggleUserStatus(String id, String updater) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setStatus(!user.isStatus());

        User updatedUser = userRepository.save(user);
        activityLogService.createLog(updater, "USER_STATUS_TOGGLED",
                String.format("User %s (%s) status set to %s.", updatedUser.getUsername(), updatedUser.getId(), updatedUser.isStatus()));

        return mapToResponse(updatedUser);
    }

    // UPDATED: Return safe DTO
    public UserResponse createUser(@Valid UserManagementRequest request, String creatorId) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setStatus(request.isStatus());
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPassword(passwordEncoder.encode("ChangeMe123!"));
        user.setPasswordChangeRequired(true);

        // Handle roles logic
        Set<Role> roles = new HashSet<>();
        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            Role defaultRole = roleRepository.findByName(ERole.ROLE_STAFF)
                    .orElseThrow(() -> new ResourceNotFoundException("Default role ROLE_STAFF", "name", ERole.ROLE_STAFF.name()));
            roles.add(defaultRole);
        } else {
            request.getRoles().forEach(roleName -> {
                try {
                    ERole roleEnum = ERole.valueOf(roleName);
                    // Crucial check: Prevent creating a Technician via the generic path
                    if (roleEnum == ERole.ROLE_TECHNICIAN) {
                        throw new ValidationException("Use the /api/technicians endpoint to create a user with the ROLE_TECHNICIAN role.");
                    }
                    Role role = roleRepository.findByName(roleEnum)
                            .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));
                    roles.add(role);
                } catch (IllegalArgumentException e) {
                    throw new ValidationException("Invalid role: " + roleName);
                }
            });
        }
        user.setRoles(roles);
        user.setCreatedBy(creatorId);

        User savedUser = userRepository.save(user);
        activityLogService.createLog(
                creatorId, "USER_CREATED_ADMIN",
                String.format("New user %s (%s) created by admin with roles %s.", savedUser.getUsername(), savedUser.getId(), roles.stream().map(r -> r.getName().name()).toList()));

        return mapToResponse(savedUser);
    }

    // UPDATED: Return safe DTO
    public List<UserResponse> getUsersByRole(ERole role) {
        return userRepository.findByRole(role).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Corrected mapToResponse to use Collectors.toSet() for the roles Set
    private UserResponse mapToResponse(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet()); // CORRECTED TO COLLECTORS.TOSET()

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .photoUrl(user.getPhotoUrl())
                .enabled(user.isEnabled())
                .status(user.isStatus())
                .roles(roleNames)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
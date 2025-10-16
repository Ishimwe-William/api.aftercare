package com.bunsen.api.aftercare.service;

import com.bunsen.api.aftercare.dto.request.UserManagementRequest;
import com.bunsen.api.aftercare.enums.ERole;
import com.bunsen.api.aftercare.exception.BadRequestException;
import com.bunsen.api.aftercare.exception.ResourceNotFoundException;
import com.bunsen.api.aftercare.model.Role;
import com.bunsen.api.aftercare.model.User;
import com.bunsen.api.aftercare.repository.RoleRepository;
import com.bunsen.api.aftercare.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdministrationService {
    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    public AdministrationService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
    public User updateUser(String id, UserManagementRequest request) {
        User user = getUserById(id);
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setEnabled(request.isEnabled());

        Set<Role> roles = new HashSet<>();
        request.getRoles().forEach(roleName -> {
            Role role = roleRepository.findByName(ERole.valueOf(roleName))
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
            roles.add(role);
        });
        user.setRoles(roles);

        return userRepository.save(user);
    }

    public void deleteUser(String id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }

    public User toggleUserStatus(String id) {
        User user = getUserById(id);
        user.setEnabled(!user.isEnabled());
        return userRepository.save(user);
    }

    public User createUser(@Valid UserManagementRequest request) {
        // Validate if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username is already taken");
        }

        // Validate if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already in use");
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setEnabled(request.isEnabled());

        // Set default password that must be changed on first login
        user.setPassword(passwordEncoder.encode("ChangeMe123!"));
        user.setPasswordChangeRequired(true);

        // Handle roles
        Set<Role> roles = new HashSet<>();
        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            // If no roles provided, assign default ROLE_USER
            Role userRole = roleRepository.findByName(ERole.ROLE_TECHNICIAN)
                    .orElseThrow(() -> new RuntimeException("Default role not found."));
            roles.add(userRole);
        } else {
            request.getRoles().forEach(roleName -> {
                try {
                    ERole roleEnum = ERole.valueOf(roleName);
                    Role role = roleRepository.findByName(roleEnum)
                            .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
                    roles.add(role);
                } catch (IllegalArgumentException e) {
                    throw new BadRequestException("Invalid role: " + roleName);
                }
            });
        }
        user.setRoles(roles);

        // Create audit fields
        user.setCreatedBy("SYSTEM");

        return userRepository.save(user);
    }

    public List<User> getUsersByRole(ERole role) {
        return userRepository.findByRole(role);
    }
}
package com.bunsen.api.aftercare.service;

import com.bunsen.api.aftercare.dto.request.TechnicianPerformanceResponse;
import com.bunsen.api.aftercare.dto.request.TechnicianRequest;
import com.bunsen.api.aftercare.dto.request.TechnicianUpdateRequest;
import com.bunsen.api.aftercare.dto.response.TechnicianResponse;
import com.bunsen.api.aftercare.dto.response.TechnicianWorkloadResponse;
import com.bunsen.api.aftercare.enums.ERole;
import com.bunsen.api.aftercare.exception.DuplicateResourceException;
import com.bunsen.api.aftercare.exception.ResourceNotFoundException;
import com.bunsen.api.aftercare.exception.ValidationException;
import com.bunsen.api.aftercare.model.Role;
import com.bunsen.api.aftercare.model.ServiceTask;
import com.bunsen.api.aftercare.model.User;
import com.bunsen.api.aftercare.repository.RoleRepository;
import com.bunsen.api.aftercare.repository.ServiceTaskRepository;
import com.bunsen.api.aftercare.repository.TechnicianRepository;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TechnicianService {

    private final TechnicianRepository technicianRepository;
    private final RoleRepository roleRepository;
    private final ServiceTaskRepository serviceTaskRepository;
    private final PasswordEncoder passwordEncoder;
    private final SystemService systemService;
    private final ActivityLogService activityLogService;

    public TechnicianService(TechnicianRepository technicianRepository,
                             RoleRepository roleRepository,
                             ServiceTaskRepository serviceTaskRepository,
                             PasswordEncoder passwordEncoder,
                             SystemService systemService,
                             ActivityLogService activityLogService1) {
        this.technicianRepository = technicianRepository;
        this.roleRepository = roleRepository;
        this.serviceTaskRepository = serviceTaskRepository;
        this.passwordEncoder = passwordEncoder;
        this.systemService = systemService;
        this.activityLogService = activityLogService1;
    }

    public List<TechnicianResponse> getAllTechnicians() {
        List<User> technicians = technicianRepository.findAllTechnicians();
        return technicians.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public TechnicianResponse getTechnicianById(String id) {
        User technician = findTechnicianById(id);
        return convertToResponse(technician);
    }

    public List<TechnicianResponse> getAvailableTechnicians() {
        List<User> technicians = technicianRepository.findAvailableTechnicians();
        return technicians.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<TechnicianResponse> getTechniciansByStatus(boolean status) {
        List<User> technicians = technicianRepository.findTechniciansByStatus(status);
        return technicians.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<TechnicianWorkloadResponse> getTechniciansOrderedByWorkload() {
        List<User> technicians = technicianRepository.findTechniciansOrderedByWorkload();
        return technicians.stream()
                .map(tech -> {
                    Long activeCount = technicianRepository.countActiveTasksByTechnician(tech.getId());
                    return TechnicianWorkloadResponse.builder()
                            .technicianId(tech.getId())
                            .username(tech.getUsername())
                            .fullName(tech.getFullName())
                            .activeTasksCount(activeCount)
                            .available(tech.isStatus() && tech.isEnabled())
                            .status(tech.isStatus() ? "online" : "offline")
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<TechnicianResponse> searchTechnicians(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllTechnicians();
        }
        List<User> technicians = technicianRepository.searchTechnicians(searchTerm.trim());
        return technicians.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public TechnicianResponse createTechnician(@Valid TechnicianRequest request, String creatorId) {
        // Use DuplicateResourceException
        if (technicianRepository.findAll().stream()
                .anyMatch(u -> u.getUsername().equals(request.getUsername()))) {
            throw new DuplicateResourceException("Technician", "username", request.getUsername());
        }

        // Use DuplicateResourceException
        if (technicianRepository.findAll().stream()
                .anyMatch(u -> u.getEmail().equals(request.getEmail()))) {
            throw new DuplicateResourceException("Technician", "email", request.getEmail());
        }

        User technician = new User();
        technician.setId(UUID.randomUUID().toString());
        technician.setUsername(request.getUsername());
        technician.setEmail(request.getEmail());
        technician.setFullName(request.getFullName());
        technician.setPhoneNumber(request.getPhoneNumber());
        technician.setPhotoUrl(request.getPhotoUrl());
        technician.setEnabled(request.isEnabled());
        technician.setStatus(request.isStatus());

        // Set default password
        technician.setPassword(passwordEncoder.encode("ChangeMe123!"));
        technician.setPasswordChangeRequired(true);

        // Assign TECHNICIAN role
        Role technicianRole = roleRepository.findByName(ERole.ROLE_TECHNICIAN)
                // Use ResourceNotFoundException for required dependency
                .orElseThrow(() -> new ResourceNotFoundException("Technician role", "name", ERole.ROLE_TECHNICIAN.name()));
        Set<Role> roles = new HashSet<>();
        roles.add(technicianRole);
        technician.setRoles(roles);

        technician.setCreatedBy("ADMIN");

        User savedTechnician = technicianRepository.save(technician);

        activityLogService.createLog(creatorId, "TECHNICIAN_CREATED",
                String.format("New technician %s created. ID: %s.", savedTechnician.getFullName(), savedTechnician.getId()));

        return convertToResponse(savedTechnician);
    }

    public TechnicianResponse updateTechnician(String id, @Valid TechnicianUpdateRequest request, String updatorId) {
        User technician = findTechnicianById(id);

        if (request.getEmail() != null && !technician.getEmail().equals(request.getEmail())) {
            // Check if email is already in use by another user
            boolean emailExists = technicianRepository.findAll().stream()
                    .anyMatch(u -> !u.getId().equals(id) && u.getEmail().equals(request.getEmail()));
            // Use DuplicateResourceException
            if (emailExists) {
                throw new DuplicateResourceException("Technician", "email", request.getEmail());
            }
            technician.setEmail(request.getEmail());
        }

        if (request.getFullName() != null) {
            technician.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            technician.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getPhotoUrl() != null) {
            technician.setPhotoUrl(request.getPhotoUrl());
        }
        if (request.getEnabled() != null) {
            technician.setEnabled(request.getEnabled());
        }
        if (request.getStatus() != null) {
            technician.setStatus(request.getStatus());
        }

        User updatedTechnician = technicianRepository.save(technician);

        activityLogService.createLog(updatorId, "TECHNICIAN_UPDATED",
                String.format("Technician %s (%s) details updated.", updatedTechnician.getFullName(), updatedTechnician.getId()));

        return convertToResponse(updatedTechnician);
    }

    public void deleteTechnician(String id, String deleterId) {
        User technician = findTechnicianById(id);

        // CRITICAL SECURITY CHECK: Prevent deletion of the SYSTEM user
        if (systemService.isSystemUser(id)) {
            throw new ValidationException("The SYSTEM user account cannot be deleted.");
        }

        // CRITICAL SECURITY CHECK 1: Prevent self-deletion
        if (id.equals(deleterId)) {
            throw new ValidationException("Cannot delete your own user account.");
        }

        // Check if technician has active tasks
        Long activeTasks = technicianRepository.countActiveTasksByTechnician(id);
        if (activeTasks > 0) {
            // Use ValidationException for business rule violation
            throw new ValidationException("Cannot delete technician with active tasks. Please reassign tasks first.");
        }
        // Dependency Resolution: Reassign Logs
        User systemUser = systemService.getSystemUser();

        // Reassign all logs from the technician being deleted to the SYSTEM_USER
        int reassignCount = activityLogService.reassignLogs(technician.getId(), systemUser.getId());

        technicianRepository.delete(technician);

        // Log the number of reassigned records.
        activityLogService.createLog(deleterId, "TECHNICIAN_DELETED",
                String.format("Technician %s (%s) deleted. Logs reassigned: %d.",
                        technician.getFullName(), technician.getId(), reassignCount));
    }

    public TechnicianResponse toggleTechnicianStatus(String id, String updatorId) {
        User technician = findTechnicianById(id);
        technician.setStatus(!technician.isStatus());
        User updatedTechnician = technicianRepository.save(technician);

        activityLogService.createLog(updatorId, "TECHNICIAN_STATUS_TOGGLED",
                String.format("Technician %s (%s) status toggled to %s.",
                        updatedTechnician.getFullName(), updatedTechnician.getId(), updatedTechnician.isStatus() ? "Active" : "Inactive"));

        return convertToResponse(updatedTechnician);
    }

    public TechnicianPerformanceResponse getTechnicianPerformance(String id) {
        User technician = findTechnicianById(id);

        List<ServiceTask> completedTasks = serviceTaskRepository.findByTechnicianIdAndStatus(
                id, ServiceTask.TaskStatus.COMPLETED);

        List<ServiceTask> activeTasks = serviceTaskRepository.findByTechnicianId(id).stream()
                .filter(t -> t.getStatus() == ServiceTask.TaskStatus.PENDING ||
                        t.getStatus() == ServiceTask.TaskStatus.IN_PROGRESS)
                .toList();

        Long pendingCount = activeTasks.stream()
                .filter(t -> t.getStatus() == ServiceTask.TaskStatus.PENDING)
                .count();

        Long inProgressCount = activeTasks.stream()
                .filter(t -> t.getStatus() == ServiceTask.TaskStatus.IN_PROGRESS)
                .count();

        // Calculate average completion time
        Double avgCompletionTime = completedTasks.stream()
                .filter(t -> t.getCompletedAt() != null && t.getCreatedAt() != null)
                .mapToDouble(t -> java.time.Duration.between(
                        t.getCreatedAt(), t.getCompletedAt()).toHours())
                .average()
                .orElse(0.0);

        // Calculate efficiency score (tasks completed / total tasks assigned * 100)
        long totalAssigned = completedTasks.size() + activeTasks.size();
        Double efficiencyScore = totalAssigned > 0 ?
                (completedTasks.size() * 100.0) / totalAssigned : 0.0;

        return TechnicianPerformanceResponse.builder()
                .technicianId(technician.getId())
                .username(technician.getUsername())
                .fullName(technician.getFullName())
                .totalTasksCompleted((long) completedTasks.size())
                .activeTasks((long) activeTasks.size())
                .pendingTasks(pendingCount)
                .inProgressTasks(inProgressCount)
                .averageCompletionTimeHours(avgCompletionTime)
                .efficiencyScore(efficiencyScore)
                .build();
    }

    public List<TechnicianPerformanceResponse> getAllTechniciansPerformance() {
        List<User> technicians = technicianRepository.findAllTechnicians();
        return technicians.stream()
                .map(tech -> getTechnicianPerformance(tech.getId()))
                .collect(Collectors.toList());
    }

    public Long getActiveTaskCount(String technicianId) {
        findTechnicianById(technicianId); // Validate technician exists
        return technicianRepository.countActiveTasksByTechnician(technicianId);
    }

    public Long getCompletedTaskCount(String technicianId) {
        findTechnicianById(technicianId); // Validate technician exists
        return serviceTaskRepository.countCompletedTasksByTechnician(technicianId);
    }

    // Helper methods
    private User findTechnicianById(String id) {
        User user = technicianRepository.findById(id)
                // Use ResourceNotFoundException with clear message
                .orElseThrow(() -> new ResourceNotFoundException("Technician", "id", id));

        // Verify user has TECHNICIAN role
        Boolean isTechnician = technicianRepository.isTechnician(id);
        if (isTechnician == null || !isTechnician) {
            // Use ResourceNotFoundException for business entity not found (i.e., not a Technician)
            throw new ResourceNotFoundException("Technician", "id", id + " (Role check failed)");
        }

        return user;
    }

    private TechnicianResponse convertToResponse(User technician) {
        Long activeTasks = technicianRepository.countActiveTasksByTechnician(technician.getId());
        Long completedTasks = serviceTaskRepository.countCompletedTasksByTechnician(technician.getId());

        Set<String> roleNames = technician.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());

        return TechnicianResponse.builder()
                .id(technician.getId())
                .username(technician.getUsername())
                .email(technician.getEmail())
                .fullName(technician.getFullName())
                .phoneNumber(technician.getPhoneNumber())
                .photoUrl(technician.getPhotoUrl())
                .enabled(technician.isEnabled())
                .status(technician.isStatus())
                .roles(roleNames)
                .activeTasks(activeTasks)
                .completedTasks(completedTasks)
                .createdAt(technician.getCreatedAt())
                .updatedAt(technician.getUpdatedAt())
                .build();
    }
}

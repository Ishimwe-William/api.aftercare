package com.bunsen.api.aftercare.service;

import com.bunsen.api.aftercare.dto.request.TechnicianPerformanceResponse;
import com.bunsen.api.aftercare.dto.request.TechnicianRequest;
import com.bunsen.api.aftercare.dto.request.TechnicianUpdateRequest;
import com.bunsen.api.aftercare.dto.response.TechnicianResponse;
import com.bunsen.api.aftercare.dto.response.TechnicianWorkloadResponse;
import com.bunsen.api.aftercare.enums.ERole;
import com.bunsen.api.aftercare.exception.BadRequestException;
import com.bunsen.api.aftercare.exception.ResourceNotFoundException;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TechnicianService {

    private final TechnicianRepository technicianRepository;
    private final RoleRepository roleRepository;
    private final ServiceTaskRepository serviceTaskRepository;
    private final PasswordEncoder passwordEncoder;

    public TechnicianService(TechnicianRepository technicianRepository,
                             RoleRepository roleRepository,
                             ServiceTaskRepository serviceTaskRepository,
                             PasswordEncoder passwordEncoder) {
        this.technicianRepository = technicianRepository;
        this.roleRepository = roleRepository;
        this.serviceTaskRepository = serviceTaskRepository;
        this.passwordEncoder = passwordEncoder;
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

    public TechnicianResponse createTechnician(@Valid TechnicianRequest request) {
        // Validate username
        if (technicianRepository.existsById(request.getUsername()) ||
                technicianRepository.findAll().stream()
                        .anyMatch(u -> u.getUsername().equals(request.getUsername()))) {
            throw new BadRequestException("Username is already taken");
        }

        // Validate email
        if (technicianRepository.findAll().stream()
                .anyMatch(u -> u.getEmail().equals(request.getEmail()))) {
            throw new BadRequestException("Email is already in use");
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
                .orElseThrow(() -> new RuntimeException("Technician role not found"));
        Set<Role> roles = new HashSet<>();
        roles.add(technicianRole);
        technician.setRoles(roles);

        technician.setCreatedBy("ADMIN");

        User savedTechnician = technicianRepository.save(technician);
        return convertToResponse(savedTechnician);
    }

    public TechnicianResponse updateTechnician(String id, @Valid TechnicianUpdateRequest request) {
        User technician = findTechnicianById(id);

        if (request.getEmail() != null) {
            // Check if email is already in use by another user
            boolean emailExists = technicianRepository.findAll().stream()
                    .anyMatch(u -> !u.getId().equals(id) && u.getEmail().equals(request.getEmail()));
            if (emailExists) {
                throw new BadRequestException("Email is already in use");
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
        return convertToResponse(updatedTechnician);
    }

    public void deleteTechnician(String id) {
        User technician = findTechnicianById(id);

        // Check if technician has active tasks
        Long activeTasks = technicianRepository.countActiveTasksByTechnician(id);
        if (activeTasks > 0) {
            throw new BadRequestException("Cannot delete technician with active tasks. Please reassign tasks first.");
        }

        technicianRepository.delete(technician);
    }

    public TechnicianResponse toggleTechnicianStatus(String id) {
        User technician = findTechnicianById(id);
        technician.setStatus(!technician.isStatus());
        User updatedTechnician = technicianRepository.save(technician);
        return convertToResponse(updatedTechnician);
    }

    public TechnicianPerformanceResponse getTechnicianPerformance(String id) {
        User technician = findTechnicianById(id);

        List<ServiceTask> completedTasks = serviceTaskRepository.findByTechnicianIdAndStatus(
                id, ServiceTask.TaskStatus.COMPLETED);

        List<ServiceTask> activeTasks = serviceTaskRepository.findByTechnicianId(id).stream()
                .filter(t -> t.getStatus() == ServiceTask.TaskStatus.PENDING ||
                        t.getStatus() == ServiceTask.TaskStatus.IN_PROGRESS)
                .collect(Collectors.toList());

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
                .orElseThrow(() -> new ResourceNotFoundException("Technician not found with id: " + id));

        // Verify user has TECHNICIAN role
        Boolean isTechnician = technicianRepository.isTechnician(id);
        if (!isTechnician) {
            throw new ResourceNotFoundException("User with id " + id + " is not a technician");
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
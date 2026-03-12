package com.bunsen.api.aftercare.service;

import com.bunsen.api.aftercare.dto.TechnicianDTO.*;
import com.bunsen.api.aftercare.enums.ERole;
import com.bunsen.api.aftercare.enums.ETaskStatus;
import com.bunsen.api.aftercare.exception.DuplicateResourceException;
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
        return technicianRepository.findAllTechnicians().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public TechnicianResponse getTechnicianById(String id) {
        return convertToResponse(findTechnicianById(id));
    }

    public List<TechnicianResponse> getAvailableTechnicians() {
        return technicianRepository.findAvailableTechnicians().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<TechnicianResponse> getTechniciansByStatus(boolean status) {
        return technicianRepository.findTechniciansByStatus(status).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<TechnicianWorkloadResponse> getTechniciansOrderedByWorkload() {
        return technicianRepository.findTechniciansOrderedByWorkload().stream()
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
        return technicianRepository.searchTechnicians(searchTerm.trim()).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public TechnicianResponse createTechnician(@Valid TechnicianRequest request, String creatorId) {
        if (technicianRepository.findAll().stream()
                .anyMatch(u -> u.getUsername().equals(request.getUsername()))) {
            throw new DuplicateResourceException("Technician", "username", request.getUsername());
        }
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
        technician.setSpeciality(request.getSpeciality()); // ← NEW

        technician.setPassword(passwordEncoder.encode("ChangeMe123!"));
        technician.setPasswordChangeRequired(true);

        Role technicianRole = roleRepository.findByName(ERole.ROLE_TECHNICIAN)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Technician role", "name", ERole.ROLE_TECHNICIAN.name()));
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
            boolean emailExists = technicianRepository.findAll().stream()
                    .anyMatch(u -> !u.getId().equals(id) && u.getEmail().equals(request.getEmail()));
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
        // ← NEW: update speciality when provided
        if (request.getSpeciality() != null) {
            technician.setSpeciality(request.getSpeciality());
        }

        User updatedTechnician = technicianRepository.save(technician);

        activityLogService.createLog(updatorId, "TECHNICIAN_UPDATED",
                String.format("Technician %s (%s) updated.", updatedTechnician.getFullName(), updatedTechnician.getId()));

        return convertToResponse(updatedTechnician);
    }

    public void deleteTechnician(String id, String deleterId) {
        User technician = findTechnicianById(id);
        User systemUser = systemService.getSystemUser();
        int reassignCount = activityLogService.reassignLogs(technician.getId(), systemUser.getId());
        technicianRepository.delete(technician);
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
                        updatedTechnician.getFullName(), updatedTechnician.getId(),
                        updatedTechnician.isStatus() ? "Active" : "Inactive"));
        return convertToResponse(updatedTechnician);
    }

    public TechnicianPerformanceResponse getTechnicianPerformance(String id) {
        User technician = findTechnicianById(id);

        List<ServiceTask> completedTasks = serviceTaskRepository.findByTechnicianIdAndStatus(id, ETaskStatus.COMPLETED);
        List<ServiceTask> activeTasks = serviceTaskRepository.findByTechnicianId(id).stream()
                .filter(t -> t.getStatus() == ETaskStatus.PENDING || t.getStatus() == ETaskStatus.IN_PROGRESS)
                .toList();

        Long pendingCount   = activeTasks.stream().filter(t -> t.getStatus() == ETaskStatus.PENDING).count();
        Long inProgressCount = activeTasks.stream().filter(t -> t.getStatus() == ETaskStatus.IN_PROGRESS).count();

        List<ServiceTask> onTimeTasks  = new ArrayList<>();
        List<ServiceTask> overdueTasks = new ArrayList<>();

        for (ServiceTask task : completedTasks) {
            if (task.getCompletedAt() != null && task.getDueTime() != null) {
                if (task.getCompletedAt().isAfter(task.getDueTime())) {
                    overdueTasks.add(task);
                } else {
                    onTimeTasks.add(task);
                }
            } else if (task.getCompletedAt() != null) {
                onTimeTasks.add(task);
            }
        }

        double avgCompletionTime = completedTasks.stream()
                .filter(t -> t.getCompletedAt() != null && t.getCreatedAt() != null)
                .mapToDouble(t -> java.time.Duration.between(t.getCreatedAt(), t.getCompletedAt()).toHours())
                .average().orElse(0.0);

        double avgDelay = overdueTasks.stream()
                .filter(t -> t.getCompletedAt() != null && t.getDueTime() != null)
                .mapToDouble(t -> java.time.Duration.between(t.getDueTime(), t.getCompletedAt()).toHours())
                .average().orElse(0.0);

        double onTimeRate = completedTasks.isEmpty() ? 0.0 :
                (onTimeTasks.size() * 100.0) / completedTasks.size();

        long totalAssigned = completedTasks.size() + activeTasks.size();
        double baseEfficiency = totalAssigned > 0 ? (completedTasks.size() * 100.0) / totalAssigned : 0.0;
        double overduePenalty = completedTasks.isEmpty() ? 0.0 :
                (overdueTasks.size() * 100.0) / completedTasks.size();
        double efficiencyScore = Math.max(0.0, baseEfficiency - (overduePenalty * 0.5));

        return TechnicianPerformanceResponse.builder()
                .technicianId(technician.getId())
                .username(technician.getUsername())
                .fullName(technician.getFullName())
                .totalTasksCompleted((long) completedTasks.size())
                .activeTasks((long) activeTasks.size())
                .pendingTasks(pendingCount)
                .inProgressTasks(inProgressCount)
                .overdueTasksCount((long) overdueTasks.size())
                .onTimeTasksCount((long) onTimeTasks.size())
                .averageCompletionTimeHours(Math.round(avgCompletionTime * 100.0) / 100.0)
                .averageDelayHours(Math.round(avgDelay * 100.0) / 100.0)
                .efficiencyScore(Math.round(efficiencyScore * 100.0) / 100.0)
                .onTimeCompletionRate(Math.round(onTimeRate * 100.0) / 100.0)
                .build();
    }

    public List<TechnicianPerformanceResponse> getAllTechniciansPerformance() {
        return technicianRepository.findAllTechnicians().stream()
                .map(tech -> getTechnicianPerformance(tech.getId()))
                .collect(Collectors.toList());
    }

    public Long getActiveTaskCount(String technicianId) {
        findTechnicianById(technicianId);
        return technicianRepository.countActiveTasksByTechnician(technicianId);
    }

    public Long getCompletedTaskCount(String technicianId) {
        findTechnicianById(technicianId);
        return serviceTaskRepository.countCompletedTasksByTechnician(technicianId);
    }

    private User findTechnicianById(String id) {
        User user = technicianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technician", "id", id));
        Boolean isTechnician = technicianRepository.isTechnician(id);
        if (isTechnician == null || !isTechnician) {
            throw new ResourceNotFoundException("Technician", "id", id + " (Role check failed)");
        }
        return user;
    }

    private TechnicianResponse convertToResponse(User technician) {
        Long activeTasks    = technicianRepository.countActiveTasksByTechnician(technician.getId());
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
                .speciality(technician.getSpeciality()) // ← NEW
                .createdAt(technician.getCreatedAt())
                .updatedAt(technician.getUpdatedAt())
                .build();
    }
}
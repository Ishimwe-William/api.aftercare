package com.bunsen.api.aftercare.service;

import com.bunsen.api.aftercare.dto.request.ServiceTaskRequest;
import com.bunsen.api.aftercare.dto.request.TaskStatusUpdateRequest;
import com.bunsen.api.aftercare.dto.response.ServiceTaskResponse;
import com.bunsen.api.aftercare.dto.response.TaskStatisticsResponse;
import com.bunsen.api.aftercare.exception.ResourceNotFoundException;
import com.bunsen.api.aftercare.exception.TaskStatusException;
import com.bunsen.api.aftercare.exception.ValidationException;
import com.bunsen.api.aftercare.model.Motorcycle;
import com.bunsen.api.aftercare.model.ServiceTask;
import com.bunsen.api.aftercare.model.User;
import com.bunsen.api.aftercare.repository.MotorcycleRepository;
import com.bunsen.api.aftercare.repository.ServiceTaskRepository;
import com.bunsen.api.aftercare.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ServiceTaskService {
    private static final Logger logger = LoggerFactory.getLogger(ServiceTaskService.class);

    private final ServiceTaskRepository serviceTaskRepository;
    private final UserRepository userRepository;
    private final MotorcycleRepository motorcycleRepository;

    public ServiceTaskService(ServiceTaskRepository serviceTaskRepository,
                              UserRepository userRepository,
                              MotorcycleRepository motorcycleRepository) {
        this.serviceTaskRepository = serviceTaskRepository;
        this.userRepository = userRepository;
        this.motorcycleRepository = motorcycleRepository;
    }

    @Transactional
    public ServiceTaskResponse createTask(ServiceTaskRequest request) {
        logger.info("Creating new service task for motorcycle: {}", request.getMotorcycleId());

        Motorcycle motorcycle = motorcycleRepository.findById(request.getMotorcycleId())
                .orElseThrow(() -> new ResourceNotFoundException("Motorcycle", "id", request.getMotorcycleId()));

        User technician = userRepository.findById(request.getTechnicianId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getTechnicianId()));

        ServiceTask task = new ServiceTask();
        task.setTaskId(UUID.randomUUID().toString());
        task.setMotorcycle(motorcycle);
        task.setTechnician(technician);
        task.setIssueType(request.getIssueType());
        task.setDescription(request.getDescription());
        task.setNotes(request.getNotes());
        task.setLaborHours(request.getLaborHours() != null ? request.getLaborHours() : BigDecimal.ZERO);
        task.setEstimatedTime(request.getEstimatedTime());
        task.setDueTime(request.getDueTime());
        task.setAssignedAt(LocalDateTime.now());
        task.setStatus(ServiceTask.TaskStatus.PENDING);

        ServiceTask savedTask = serviceTaskRepository.save(task);
        logger.info("Service task created successfully with ID: {}", savedTask.getTaskId());

        return mapToResponse(savedTask);
    }

    @Transactional(readOnly = true)
    public ServiceTaskResponse getTaskById(String taskId) {
        ServiceTask task = serviceTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceTask", "taskId", taskId));
        return mapToResponse(task);
    }

    @Transactional(readOnly = true)
    public Page<ServiceTaskResponse> getAllTasks(Pageable pageable) {
        return serviceTaskRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<ServiceTaskResponse> getTasksByTechnician(String technicianId) {
        return serviceTaskRepository.findByTechnicianId(technicianId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServiceTaskResponse> getTasksByMotorcycle(String motorcycleId) {
        return serviceTaskRepository.findByMotorcycleMotorcycleId(motorcycleId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServiceTaskResponse> getTasksByStatus(ServiceTask.TaskStatus status) {
        return serviceTaskRepository.findByStatus(status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServiceTaskResponse> getTasksByTechnicianAndStatus(String technicianId,
                                                                   ServiceTask.TaskStatus status) {
        return serviceTaskRepository.findByTechnicianIdAndStatus(technicianId, status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ServiceTaskResponse updateTask(String taskId, ServiceTaskRequest request) {
        logger.info("Updating service task: {}", taskId);

        ServiceTask task = serviceTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceTask", "taskId", taskId));

        if (task.getStatus() == ServiceTask.TaskStatus.COMPLETED) {
            throw new TaskStatusException(task.getStatus().name(), "update");
        }

        if (!request.getMotorcycleId().equals(task.getMotorcycle().getMotorcycleId())) {
            Motorcycle motorcycle = motorcycleRepository.findById(request.getMotorcycleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Motorcycle", "id", request.getMotorcycleId()));
            task.setMotorcycle(motorcycle);
        }

        if (!request.getTechnicianId().equals(task.getTechnician().getId())) {
            User technician = userRepository.findById(request.getTechnicianId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getTechnicianId()));
            task.setTechnician(technician);
        }

        task.setIssueType(request.getIssueType());
        task.setDescription(request.getDescription());
        task.setNotes(request.getNotes());

        if (request.getLaborHours() != null) {
            task.setLaborHours(request.getLaborHours());
        }

        task.setEstimatedTime(request.getEstimatedTime());
        task.setDueTime(request.getDueTime());

        ServiceTask updatedTask = serviceTaskRepository.save(task);
        logger.info("Service task updated successfully: {}", taskId);

        return mapToResponse(updatedTask);
    }

    @Transactional
    public ServiceTaskResponse updateTaskStatus(String taskId, TaskStatusUpdateRequest request) {
        logger.info("Updating task status for task: {} to {}", taskId, request.getStatus());

        ServiceTask task = serviceTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceTask", "taskId", taskId));

        ServiceTask.TaskStatus currentStatus = task.getStatus();
        ServiceTask.TaskStatus newStatus = request.getStatus();

        if (currentStatus == ServiceTask.TaskStatus.COMPLETED) {
            throw new TaskStatusException(currentStatus.name(), "update");
        }

        validateStatusTransition(currentStatus, newStatus);

        switch (newStatus) {
            case IN_PROGRESS:
                task.setStartedAt(LocalDateTime.now());
                break;
            case COMPLETED:
                task.setCompletedAt(LocalDateTime.now());
                break;
        }

        task.setStatus(newStatus);

        if (request.getNotes() != null) {
            task.setNotes(request.getNotes());
        }

        if (request.getLaborHours() != null) {
            task.setLaborHours(request.getLaborHours());
        }

        ServiceTask updatedTask = serviceTaskRepository.save(task);
        logger.info("Task status updated successfully: {} -> {}", currentStatus, newStatus);

        return mapToResponse(updatedTask);
    }

    @Transactional
    public void deleteTask(String taskId) {
        logger.info("Deleting service task: {}", taskId);

        ServiceTask task = serviceTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceTask", "taskId", taskId));

        if (task.getStatus() != ServiceTask.TaskStatus.PENDING) {
            throw new TaskStatusException(task.getStatus().name(), "delete");
        }

        serviceTaskRepository.delete(task);
        logger.info("Service task deleted successfully: {}", taskId);
    }

    @Transactional(readOnly = true)
    public Page<ServiceTaskResponse> getOverdueTasks(Pageable pageable) {
        return serviceTaskRepository.findOverdueTasks(LocalDateTime.now(), pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ServiceTaskResponse> getCompletedTasksBetween(LocalDateTime startDate,
                                                              LocalDateTime endDate,
                                                              Pageable pageable) {
        return serviceTaskRepository.findCompletedTasksBetween(startDate, endDate, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ServiceTaskResponse> getTechnicianTasksInDateRange(String technicianId,
                                                                   LocalDateTime startDate,
                                                                   LocalDateTime endDate,
                                                                   Pageable pageable) {
        return serviceTaskRepository.findTechnicianTasksInDateRange(technicianId, startDate, endDate, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public TaskStatisticsResponse getTaskStatistics() {
        List<ServiceTask> allTasks = serviceTaskRepository.findAll();

        long totalTasks = allTasks.size();
        long pendingTasks = allTasks.stream()
                .filter(t -> t.getStatus() == ServiceTask.TaskStatus.PENDING)
                .count();
        long inProgressTasks = allTasks.stream()
                .filter(t -> t.getStatus() == ServiceTask.TaskStatus.IN_PROGRESS)
                .count();
        long completedTasks = allTasks.stream()
                .filter(t -> t.getStatus() == ServiceTask.TaskStatus.COMPLETED)
                .count();

        LocalDateTime now = LocalDateTime.now();
        long overdueTasks = allTasks.stream()
                .filter(t -> t.getDueTime() != null &&
                        t.getDueTime().isBefore(now) &&
                        (t.getStatus() == ServiceTask.TaskStatus.PENDING ||
                                t.getStatus() == ServiceTask.TaskStatus.IN_PROGRESS))
                .count();

        Double averageCompletionTime = serviceTaskRepository.calculateAverageCompletionTimeInHours();

        long totalLaborHours = allTasks.stream()
                .map(ServiceTask::getLaborHours)
                .filter(hours -> hours != null)
                .mapToLong(BigDecimal::longValue)
                .sum();

        return TaskStatisticsResponse.builder()
                .totalTasks(totalTasks)
                .pendingTasks(pendingTasks)
                .inProgressTasks(inProgressTasks)
                .completedTasks(completedTasks)
                .overdueTasks(overdueTasks)
                .averageCompletionTimeInHours(averageCompletionTime)
                .totalLaborHours(totalLaborHours)
                .build();
    }

    @Transactional(readOnly = true)
    public Long getCompletedTaskCountByTechnician(String technicianId) {
        return serviceTaskRepository.countCompletedTasksByTechnician(technicianId);
    }

    private void validateStatusTransition(ServiceTask.TaskStatus current, ServiceTask.TaskStatus target) {
        switch (target) {
            case IN_PROGRESS:
                if (current != ServiceTask.TaskStatus.PENDING && current != ServiceTask.TaskStatus.PAUSED) {
                    throw new TaskStatusException(current.name(), "start");
                }
                break;
            case PAUSED:
                if (current != ServiceTask.TaskStatus.IN_PROGRESS) {
                    throw new TaskStatusException(current.name(), "pause");
                }
                break;
            case COMPLETED:
                if (current != ServiceTask.TaskStatus.IN_PROGRESS) {
                    throw new TaskStatusException(current.name(), "complete");
                }
                break;
            case PENDING:
                throw new ValidationException("Cannot transition back to PENDING status");
        }
    }

    private ServiceTaskResponse mapToResponse(ServiceTask task) {
        Long durationInHours = null;
        if (task.getCompletedAt() != null && task.getStartedAt() != null) {
            durationInHours = Duration.between(task.getStartedAt(), task.getCompletedAt()).toHours();
        }

        Boolean isOverdue = false;
        if (task.getDueTime() != null &&
                (task.getStatus() == ServiceTask.TaskStatus.PENDING ||
                        task.getStatus() == ServiceTask.TaskStatus.IN_PROGRESS)) {
            isOverdue = task.getDueTime().isBefore(LocalDateTime.now());
        }

        return ServiceTaskResponse.builder()
                .taskId(task.getTaskId())
                .motorcycleId(task.getMotorcycle().getMotorcycleId())
                .motorcyclePlateNumber(task.getMotorcycle().getPlateNumber())
                .technicianId(task.getTechnician().getId())
                .technicianName(task.getTechnician().getFullName())
                .issueType(task.getIssueType())
                .description(task.getDescription())
                .status(task.getStatus())
                .assignedAt(task.getAssignedAt())
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                .notes(task.getNotes())
                .laborHours(task.getLaborHours())
                .estimatedTime(task.getEstimatedTime())
                .dueTime(task.getDueTime())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .durationInHours(durationInHours)
                .isOverdue(isOverdue)
                .build();
    }
}
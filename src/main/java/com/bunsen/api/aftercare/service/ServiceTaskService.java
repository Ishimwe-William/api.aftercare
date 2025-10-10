package com.bunsen.api.aftercare.service;

import com.bunsen.api.aftercare.exception.ResourceNotFoundException;
import com.bunsen.api.aftercare.exception.TaskStatusException;
import com.bunsen.api.aftercare.exception.ValidationException;
import com.bunsen.api.aftercare.model.ServiceTask;
import com.bunsen.api.aftercare.model.User;
import com.bunsen.api.aftercare.repository.ServiceTaskRepository;
import com.bunsen.api.aftercare.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ServiceTaskService {
    private static final Logger logger = LoggerFactory.getLogger(ServiceTaskService.class);
    private final ServiceTaskRepository serviceTaskRepository;
    private final UserRepository userRepository;

    public ServiceTaskService(ServiceTaskRepository serviceTaskRepository, UserRepository userRepository) {
        this.serviceTaskRepository = serviceTaskRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ServiceTask createTask(ServiceTask task, String technicianId) {
        if (task.getIssueType() == null || task.getIssueType().isBlank()) {
            throw new ValidationException("Issue type is required");
        }
        User technician = userRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", technicianId));
        task.setTaskId(UUID.randomUUID().toString());
        task.setTechnician(technician);
        task.setAssignedAt(LocalDateTime.now());
        task.setStatus(ServiceTask.TaskStatus.PENDING);
        return serviceTaskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public ServiceTask getTaskById(String taskId) {
        return serviceTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceTask", "taskId", taskId));
    }

    @Transactional(readOnly = true)
    public List<ServiceTask> getTasksByTechnician(String technicianId) {
        return serviceTaskRepository.findByTechnicianId(technicianId);
    }

    @Transactional
    public ServiceTask updateTaskStatus(String taskId, ServiceTask.TaskStatus newStatus) {
        ServiceTask task = getTaskById(taskId);
        ServiceTask.TaskStatus currentStatus = task.getStatus();
        if (currentStatus == ServiceTask.TaskStatus.COMPLETED) {
            throw new TaskStatusException(currentStatus.name(), "update");
        }
        switch (newStatus) {
            case IN_PROGRESS:
                if (currentStatus != ServiceTask.TaskStatus.PENDING) {
                    throw new TaskStatusException(currentStatus.name(), "start");
                }
                task.setStartedAt(LocalDateTime.now());
                break;
            case PAUSED:
                if (currentStatus != ServiceTask.TaskStatus.IN_PROGRESS) {
                    throw new TaskStatusException(currentStatus.name(), "pause");
                }
                break;
            case COMPLETED:
                if (currentStatus != ServiceTask.TaskStatus.IN_PROGRESS) {
                    throw new TaskStatusException(currentStatus.name(), "complete");
                }
                task.setCompletedAt(LocalDateTime.now());
                break;
            default:
                throw new ValidationException("Invalid status transition");
        }
        task.setStatus(newStatus);
        return serviceTaskRepository.save(task);
    }

    @Transactional
    public void deleteTask(String taskId) {
        ServiceTask task = getTaskById(taskId);
        if (task.getStatus() != ServiceTask.TaskStatus.PENDING) {
            throw new TaskStatusException(task.getStatus().name(), "delete");
        }
        serviceTaskRepository.delete(task);
    }

    @Transactional(readOnly = true)
    public Page<ServiceTask> getOverdueTasks(Pageable pageable) {
        return serviceTaskRepository.findOverdueTasks(LocalDateTime.now(), pageable);
    }

    @Transactional(readOnly = true)
    public Double getAverageCompletionTime() {
        return serviceTaskRepository.calculateAverageCompletionTimeInHours();
    }
}
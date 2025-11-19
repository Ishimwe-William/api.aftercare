package com.bunsen.api.aftercare.service;

import com.bunsen.api.aftercare.dto.ServiceTaskDTO.*;
import com.bunsen.api.aftercare.enums.ETaskStatus;
import com.bunsen.api.aftercare.exception.ResourceNotFoundException;
import com.bunsen.api.aftercare.exception.TaskStatusException;
import com.bunsen.api.aftercare.exception.UnauthorizedException;
import com.bunsen.api.aftercare.model.Invoice;
import com.bunsen.api.aftercare.model.Motorcycle;
import com.bunsen.api.aftercare.model.ServiceTask;
import com.bunsen.api.aftercare.model.User;
import com.bunsen.api.aftercare.repository.MotorcycleRepository;
import com.bunsen.api.aftercare.repository.ServiceTaskRepository;
import com.bunsen.api.aftercare.repository.UserRepository;
import com.bunsen.api.aftercare.util.EntityMapperUtil;
import com.bunsen.api.aftercare.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ServiceTaskService {
    private static final Logger logger = LoggerFactory.getLogger(ServiceTaskService.class);

    private final ServiceTaskRepository serviceTaskRepository;
    private final UserRepository userRepository;
    private final MotorcycleRepository motorcycleRepository;
    private final ActivityLogService activityLogService;
    private final EntityMapperUtil entityMapperUtil;
    private final ValidationUtil validationUtil;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;
    private final InvoiceService invoiceService;

    public ServiceTaskService(ServiceTaskRepository serviceTaskRepository,
                              UserRepository userRepository,
                              MotorcycleRepository motorcycleRepository,
                              ActivityLogService activityLogService,
                              EntityMapperUtil entityMapperUtil,
                              ValidationUtil validationUtil, SimpMessagingTemplate messagingTemplate, EmailService emailService, InvoiceService invoiceService) {
        this.serviceTaskRepository = serviceTaskRepository;
        this.userRepository = userRepository;
        this.motorcycleRepository = motorcycleRepository;
        this.activityLogService = activityLogService;
        this.entityMapperUtil = entityMapperUtil;
        this.validationUtil = validationUtil;
        this.messagingTemplate = messagingTemplate;
        this.emailService = emailService;
        this.invoiceService = invoiceService;
    }

    @Transactional
    public ServiceTaskResponse createTask(ServiceTaskRequest request, String creatorId) {
        logger.info("Creating new service task for motorcycle: {}", request.getMotorcycleId());

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", creatorId));

        boolean isAdmin = creator.getRoles().stream()
                .anyMatch(role -> role.getName().name().equals("ROLE_ADMIN"));

        if (!isAdmin && !creatorId.equals(request.getTechnicianId())) {
            throw new UnauthorizedException("Technicians can only create tasks for themselves");
        }

        Motorcycle motorcycle = motorcycleRepository.findById(request.getMotorcycleId())
                .orElseThrow(() -> new ResourceNotFoundException("Motorcycle", "id", request.getMotorcycleId()));

        User technician = userRepository.findById(request.getTechnicianId())
                .orElseThrow(() -> new ResourceNotFoundException("Technician", "id", request.getTechnicianId()));

        ServiceTask task = new ServiceTask();
        task.setId(UUID.randomUUID().toString());
        task.setMotorcycle(motorcycle);
        task.setTechnician(technician);
        task.setIssueType(request.getIssueType());
        task.setDescription(request.getDescription());
        task.setNotes(request.getNotes());
        task.setLaborHours(request.getLaborHours() != null ? request.getLaborHours() : BigDecimal.ZERO);
        task.setEstimatedTime(request.getEstimatedTime());
        task.setDueTime(request.getDueTime());
        task.setAssignedAt(LocalDateTime.now());
        task.setStatus(ETaskStatus.PENDING);

        ServiceTask savedTask = serviceTaskRepository.save(task);
        logger.info("Service task created successfully with ID: {}", savedTask.getId());

        updateMotorcycleStatusBasedOnTasks(savedTask.getMotorcycle().getId(), creatorId);

        activityLogService.createLog(creatorId, "TASK_CREATED",
                String.format("Service task %s created and assigned to %s.", savedTask.getId(), technician.getFullName()));

        // Send email notification to assigned technician
        sendTaskAssignmentEmail(savedTask, false);

        return entityMapperUtil.mapToServiceTaskResponse(savedTask);
    }

    @Transactional(readOnly = true)
    public ServiceTaskResponse getTaskById(String taskId) {
        ServiceTask task = serviceTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceTask", "taskId", taskId));
        return entityMapperUtil.mapToServiceTaskResponse(task);
    }

    @Transactional(readOnly = true)
    public Page<ServiceTaskResponse> getAllTasks(Pageable pageable) {
        return serviceTaskRepository.findAll(pageable)
                .map(entityMapperUtil::mapToServiceTaskResponse);
    }

    @Transactional(readOnly = true)
    public List<ServiceTaskResponse> getTasksByTechnician(String technicianId) {
        return serviceTaskRepository.findByTechnicianId(technicianId).stream()
                .map(entityMapperUtil::mapToServiceTaskResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServiceTaskResponse> getTasksByMotorcycle(String motorcycleId) {
        return serviceTaskRepository.findByMotorcycleId(motorcycleId).stream()
                .map(entityMapperUtil::mapToServiceTaskResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServiceTaskResponse> getTasksByStatus(ETaskStatus status) {
        return serviceTaskRepository.findByStatus(status).stream()
                .map(entityMapperUtil::mapToServiceTaskResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServiceTaskResponse> getTasksByTechnicianAndStatus(String technicianId,
                                                                   ETaskStatus status) {
        return serviceTaskRepository.findByTechnicianIdAndStatus(technicianId, status).stream()
                .map(entityMapperUtil::mapToServiceTaskResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ServiceTaskResponse updateTask(String taskId, ServiceTaskRequest request, UserDetailsImpl principal) {
        logger.info("Updating service task: {}", taskId);

        ServiceTask task = serviceTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceTask", "taskId", taskId));

        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !principal.getId().equals(task.getTechnician().getId())) {
            throw new UnauthorizedException("You can only update your own tasks");
        }

        if (task.getStatus() == ETaskStatus.COMPLETED && !isAdmin) {
            throw new TaskStatusException(task.getStatus().name(), "update");
        }

        String oldTechnicianName = task.getTechnician().getFullName();
        boolean technicianChanged = false;

        if (!request.getMotorcycleId().equals(task.getMotorcycle().getId())) {
            Motorcycle motorcycle = motorcycleRepository.findById(request.getMotorcycleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Motorcycle", "id", request.getMotorcycleId()));
            task.setMotorcycle(motorcycle);
        }

        if (!request.getTechnicianId().equals(task.getTechnician().getId())) {
            User newTechnician = userRepository.findById(request.getTechnicianId())
                    .orElseThrow(() -> new ResourceNotFoundException("Technician", "id", request.getTechnicianId()));
            task.setTechnician(newTechnician);
            technicianChanged = true;
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

        updateMotorcycleStatusBasedOnTasks(updatedTask.getMotorcycle().getId(), principal.getId());

        if (technicianChanged) {
            // Log the reassignment
            activityLogService.createLog(principal.getId(), "TASK_REASSIGNED",
                    String.format("Task %s reassigned from %s to %s during update.",
                            taskId, oldTechnicianName, updatedTask.getTechnician().getFullName()));

            // Send email notification to new technician
            sendTaskAssignmentEmail(updatedTask, true);
        } else {
            activityLogService.createLog(updatedTask.getTechnician().getId(), "TASK_UPDATED",
                    String.format("Service task %s details updated by %s.",
                            taskId, updatedTask.getTechnician().getFullName()));
        }

        return entityMapperUtil.mapToServiceTaskResponse(updatedTask);
    }


    @Transactional
    public ServiceTaskResponse updateTaskStatus(String taskId, TaskStatusUpdateRequest request, UserDetailsImpl principal) {
        logger.info("Updating task for task: {}", taskId);

        ServiceTask task = serviceTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceTask", "taskId", taskId));

        ETaskStatus currentStatus = task.getStatus();
        String updaterId = principal.getId();

        // Check for Admin role
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // Only process status change if status is provided in request
        if (request.getStatus() != null) {
            ETaskStatus newStatus = request.getStatus();

            if (currentStatus == ETaskStatus.COMPLETED && !isAdmin) {
                throw new TaskStatusException(currentStatus.name(), "update");
            }

            // Only validate status transition if the user is NOT an Admin
            if (!isAdmin) {
                validationUtil.validateStatusTransition(currentStatus, newStatus);
            }

            switch (newStatus) {
                case IN_PROGRESS:
                    if (task.getStartedAt() == null) {
                        task.setStartedAt(LocalDateTime.now());
                    }
                    task.setCancelledAt(null);
                    break;
                case COMPLETED:
                    task.setCompletedAt(LocalDateTime.now());
                    task.setCancelledAt(null);
                    break;
                case CANCELLED:
                    task.setCancelledAt(LocalDateTime.now());
                    task.setCompletedAt(null);
                    break;
                case PAUSED:
                    // Log pause time if necessary
                    break;
                case PENDING:
                    // If admin forces PENDING, clear start/complete/cancel times
                    task.setStartedAt(null);
                    task.setCompletedAt(null);
                    task.setCancelledAt(null);
                    break;
            }

            task.setStatus(newStatus);

            // Log the status change
            activityLogService.createLog(updaterId, "TASK_STATUS_CHANGE",
                    String.format("Task %s status changed from %s to %s by user %s.",
                            taskId, currentStatus, newStatus, updaterId));

        }

        // Update notes if provided
        if (request.getNotes() != null) {
            task.setNotes(request.getNotes());
        }

        // Update labor hours if provided
        if (request.getLaborHours() != null) {
            task.setLaborHours(request.getLaborHours());

            // Log labor hours update only if status wasn't changed
            if (request.getStatus() == null) {
                activityLogService.createLog(updaterId, "TASK_LABOR_HOURS_UPDATE",
                        String.format("Task %s labor hours updated to %s by user %s.",
                                taskId, request.getLaborHours(), updaterId));
            }
        }

        ServiceTask updatedTask = serviceTaskRepository.save(task);
        logger.info("Task updated successfully: {}", taskId);

        updateMotorcycleStatusBasedOnTasks(updatedTask.getMotorcycle().getId(), principal.getId());

        ServiceTaskResponse responseDTO = entityMapperUtil.mapToServiceTaskResponse(updatedTask);
        messagingTemplate.convertAndSend("/topic/tasks", responseDTO);

        return responseDTO;
    }

    private void updateMotorcycleStatusBasedOnTasks(String motorcycleId, String updaterId) {
        long activeTaskCount = serviceTaskRepository.countByMotorcycleIdAndStatusNot(motorcycleId, ETaskStatus.COMPLETED);
        Motorcycle motorcycle = motorcycleRepository.findById(motorcycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Motorcycle", "id", motorcycleId));

        Motorcycle.MotorcycleStatus newStatus = (activeTaskCount > 0) ? Motorcycle.MotorcycleStatus.IN_SERVICE : Motorcycle.MotorcycleStatus.ACTIVE;

        if (motorcycle.getStatus() != newStatus) {
            Motorcycle.MotorcycleStatus oldStatus = motorcycle.getStatus();
            motorcycle.setStatus(newStatus);
            motorcycle.setLastServiceDate(LocalDateTime.now());
            motorcycleRepository.save(motorcycle);
            logger.info("Updated motorcycle {} status from {} to {}", motorcycleId, oldStatus, newStatus);

            activityLogService.createLog(updaterId, "MOTORCYCLE_STATUS_AUTO_UPDATE",
                    String.format("Motorcycle %s status auto-updated from %s to %s based on tasks.", motorcycle.getPlateNumber(), oldStatus, newStatus));
        }
    }

    @Transactional
    public void deleteTask(String taskId, UserDetailsImpl principal) {
        ServiceTask task = serviceTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceTask", "taskId", taskId));

        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (task.getStatus() == ETaskStatus.COMPLETED && !isAdmin) {
            throw new TaskStatusException(task.getStatus().name(), "delete");
        }
        Invoice invoice = invoiceService.getInvoiceByTaskId(taskId);

        if (invoice != null) {
            invoiceService.deleteInvoice(invoice.getInvoiceId());
            activityLogService.createLog(principal.getId(), "INVOICE_DELETED",
                    String.format("Invoice %s deleted because associated task %s was deleted.",
                            invoice.getInvoiceId(), task.getId()));
        }

        serviceTaskRepository.delete(task);
        logger.info("Service task deleted successfully: {}", taskId);

        updateMotorcycleStatusBasedOnTasks(task.getMotorcycle().getId(), principal.getId());

        activityLogService.createLog(principal.getId(), "TASK_DELETED",
                String.format("Task %s deleted. Was assigned to %s.", taskId, task.getTechnician().getFullName()));
    }

    @Transactional(readOnly = true)
    public Page<ServiceTaskResponse> getOverdueTasks(Pageable pageable) {
        return serviceTaskRepository.findOverdueTasks(LocalDateTime.now(), pageable)
                .map(entityMapperUtil::mapToServiceTaskResponse);
    }

    @Transactional(readOnly = true)
    public Page<ServiceTaskResponse> getCompletedTasksBetween(LocalDateTime startDate,
                                                              LocalDateTime endDate,
                                                              Pageable pageable) {
        return serviceTaskRepository.findCompletedTasksBetween(startDate, endDate, pageable)
                .map(entityMapperUtil::mapToServiceTaskResponse);
    }

    @Transactional(readOnly = true)
    public Page<ServiceTaskResponse> getTechnicianTasksInDateRange(String technicianId,
                                                                   LocalDateTime startDate,
                                                                   LocalDateTime endDate,
                                                                   Pageable pageable) {
        return serviceTaskRepository.findTechnicianTasksInDateRange(technicianId, startDate, endDate, pageable)
                .map(entityMapperUtil::mapToServiceTaskResponse);
    }

    @Transactional(readOnly = true)
    public TaskStatisticsResponse getTaskStatistics() {
        List<ServiceTask> allTasks = serviceTaskRepository.findAll();

        long totalTasks = allTasks.size();
        long pendingTasks = allTasks.stream()
                .filter(t -> t.getStatus() == ETaskStatus.PENDING)
                .count();
        long inProgressTasks = allTasks.stream()
                .filter(t -> t.getStatus() == ETaskStatus.IN_PROGRESS)
                .count();
        long completedTasks = allTasks.stream()
                .filter(t -> t.getStatus() == ETaskStatus.COMPLETED)
                .count();

        LocalDateTime now = LocalDateTime.now();
        long overdueTasks = allTasks.stream()
                .filter(t -> t.getDueTime() != null &&
                        t.getDueTime().isBefore(now) &&
                        (t.getStatus() == ETaskStatus.PENDING ||
                                t.getStatus() == ETaskStatus.IN_PROGRESS))
                .count();

        Double averageCompletionTime = serviceTaskRepository.calculateAverageCompletionTimeInHours();

        long totalLaborHours = allTasks.stream()
                .map(ServiceTask::getLaborHours)
                .filter(Objects::nonNull)
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

    @Transactional(readOnly = true)
    public Page<ServiceTaskResponse> getAllTasksInDateRange(LocalDateTime startDate,
                                                            LocalDateTime endDate,
                                                            Pageable pageable) {
        return serviceTaskRepository.findAllTasksInDateRange(startDate, endDate, pageable)
                .map(entityMapperUtil::mapToServiceTaskResponse);
    }

    private void sendTaskAssignmentEmail(ServiceTask task, boolean isReassignment) {
        try {
            User technician = task.getTechnician();
            String email = technician.getEmail();

            if (email == null || email.isBlank()) {
                logger.warn("Cannot send email - technician {} has no email address", technician.getId());
                return;
            }

            String subject = isReassignment
                    ? "New Task Reassigned - " + task.getIssueType()
                    : "New Task Assigned - " + task.getIssueType();

            String body = buildTaskAssignmentEmailBody(task, technician, isReassignment);

            emailService.sendEmail(email, "Aftercare App", subject, body);

            logger.info("Task assignment email sent to technician: {}", technician.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send task assignment email", e);
            // Don't throw exception - email failure shouldn't break task assignment
        }
    }

    private String buildTaskAssignmentEmailBody(ServiceTask task, User technician, boolean isReassignment) {
        StringBuilder body = new StringBuilder();

        body.append("Hello ").append(technician.getFullName()).append(",\n\n");

        if (isReassignment) {
            body.append("A task has been reassigned to you.\n\n");
        } else {
            body.append("A new task has been assigned to you.\n\n");
        }

        body.append("Task Details:\n");
        body.append("----------------------------------\n");
        body.append("Task ID: ").append(task.getId()).append("\n");
        body.append("Issue Type: ").append(task.getIssueType()).append("\n");
        body.append("Motorcycle: ").append(task.getMotorcycle().getPlateNumber()).append("\n");
        body.append("Status: ").append(task.getStatus()).append("\n");

        if (task.getDescription() != null && !task.getDescription().isBlank()) {
            body.append("Description: ").append(task.getDescription()).append("\n");
        }

        if (task.getDueTime() != null) {
            body.append("Due Date: ").append(task.getDueTime()).append("\n");
        }

        if (task.getEstimatedTime() != null) {
            body.append("Estimated Time: ").append(task.getEstimatedTime()).append(" minutes\n");
        }

        body.append("-----------------------------------\n\n");
        body.append("Please log in to the system to view full task details and update the status.\n\n");
        body.append("If you have any questions, please contact your supervisor.\n\n");

        return body.toString();
    }
}
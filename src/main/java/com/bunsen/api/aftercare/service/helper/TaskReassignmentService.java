package com.bunsen.api.aftercare.service.helper;

import com.bunsen.api.aftercare.dto.MonitoringDTO.ReassignRequest;
import com.bunsen.api.aftercare.enums.ETaskStatus;
import com.bunsen.api.aftercare.exception.BadRequestException;
import com.bunsen.api.aftercare.exception.ResourceNotFoundException;
import com.bunsen.api.aftercare.exception.ValidationException;
import com.bunsen.api.aftercare.model.ActivityLog;
import com.bunsen.api.aftercare.model.ServiceTask;
import com.bunsen.api.aftercare.model.User;
import com.bunsen.api.aftercare.repository.ActivityLogRepository;
import com.bunsen.api.aftercare.repository.ServiceTaskRepository;
import com.bunsen.api.aftercare.repository.TechnicianRepository;
import com.bunsen.api.aftercare.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service component responsible for task reassignment logic
 * Centralizes reassignment validation and execution used by multiple services
 */
@Service
@RequiredArgsConstructor
public class TaskReassignmentService {

    private static final Logger logger = LoggerFactory.getLogger(TaskReassignmentService.class);

    private final ServiceTaskRepository serviceTaskRepository;
    private final TechnicianRepository technicianRepository;
    private final ActivityLogRepository activityLogRepository;
    private final EmailTemplateBuilder emailTemplateBuilder;
    private final EmailService emailService;

    /**
     * Reassigns a task to a new technician with full validation
     *
     * @param request ReassignRequest containing taskId, newTechnicianId, and reason
     * @param sendEmail Whether to send email notification to the new technician
     * @return ReassignmentResult containing the updated task and reassignment details
     */
    @Transactional
    public ReassignmentResult reassignTask(ReassignRequest request, boolean sendEmail) {
        validateReassignRequest(request);

        ServiceTask task = getTaskById(request.getTaskId());
        validateTaskCanBeReassigned(task);

        User newTechnician = validateAndGetTechnician(request.getNewTechnicianId());
        String oldTechnicianName = task.getTechnician().getFullName();
        String oldTechnicianId = task.getTechnician().getId();

        // Perform the reassignment
        task.setTechnician(newTechnician);
        task.setUpdatedAt(LocalDateTime.now());

        ServiceTask updatedTask = serviceTaskRepository.save(task);

        // Log the reassignment
        logReassignment(updatedTask, oldTechnicianName, newTechnician, request.getReason());

        logger.info("Task {} successfully reassigned from {} to {}",
                task.getId(), oldTechnicianName, newTechnician.getFullName());

        // Send email notification if requested
        if (sendEmail) {
            sendReassignmentEmail(updatedTask, newTechnician);
        }

        return ReassignmentResult.builder()
                .task(updatedTask)
                .oldTechnicianId(oldTechnicianId)
                .oldTechnicianName(oldTechnicianName)
                .newTechnicianId(newTechnician.getId())
                .newTechnicianName(newTechnician.getFullName())
                .reason(request.getReason())
                .wasReassigned(true)
                .emailSent(sendEmail)
                .build();
    }

    /**
     * Reassigns a task during update operations (simplified version)
     *
     * @param task The task to reassign
     * @param newTechnicianId ID of the new technician
     * @param performedBy User performing the reassignment
     * @param sendEmail Whether to send email notification to the new technician
     * @return ReassignmentResult with details of the reassignment
     */
    @Transactional
    public ReassignmentResult reassignTaskDuringUpdate(ServiceTask task, String newTechnicianId,
                                                       String performedBy, boolean sendEmail) {
        if (task.getTechnician().getId().equals(newTechnicianId)) {
            return ReassignmentResult.builder()
                    .task(task)
                    .wasReassigned(false)
                    .emailSent(false)
                    .build();
        }

        User newTechnician = validateAndGetTechnician(newTechnicianId);
        String oldTechnicianName = task.getTechnician().getFullName();
        String oldTechnicianId = task.getTechnician().getId();

        task.setTechnician(newTechnician);
        task.setUpdatedAt(LocalDateTime.now());

        // Log the reassignment during update
        logReassignmentDuringUpdate(task, oldTechnicianName, newTechnician, performedBy);

        logger.info("Task {} reassigned during update from {} to {}",
                task.getId(), oldTechnicianName, newTechnician.getFullName());

        // Send email notification if requested
        if (sendEmail) {
            sendReassignmentEmail(task, newTechnician);
        }

        return ReassignmentResult.builder()
                .task(task)
                .oldTechnicianId(oldTechnicianId)
                .oldTechnicianName(oldTechnicianName)
                .newTechnicianId(newTechnician.getId())
                .newTechnicianName(newTechnician.getFullName())
                .wasReassigned(true)
                .emailSent(sendEmail)
                .build();
    }

    /**
     * Validates if a task can be reassigned
     */
    public void validateTaskCanBeReassigned(ServiceTask task) {
        if (task.getStatus() == ETaskStatus.COMPLETED) {
            throw new BadRequestException("Cannot reassign completed tasks");
        }
    }

    /**
     * Validates the reassignment request parameters
     */
    private void validateReassignRequest(ReassignRequest request) {
        if (request == null) {
            throw new BadRequestException("Reassignment request is required");
        }
        if (request.getTaskId() == null || request.getTaskId().trim().isEmpty()) {
            throw new ValidationException("Task ID is required");
        }
        if (request.getNewTechnicianId() == null || request.getNewTechnicianId().trim().isEmpty()) {
            throw new ValidationException("New technician ID is required");
        }
    }

    /**
     * Validates and retrieves a technician by ID
     */
    private User validateAndGetTechnician(String technicianId) {
        User technician = technicianRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Technician", "id", technicianId));

        Boolean isTech = technicianRepository.isTechnician(technicianId);
        if (isTech == null || !isTech) {
            throw new ValidationException("User is not a technician");
        }

        if (!technician.isEnabled() || !technician.isStatus()) {
            throw new ValidationException("Selected technician is not available");
        }

        return technician;
    }

    /**
     * Retrieves a task by ID
     */
    private ServiceTask getTaskById(String taskId) {
        return serviceTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("ServiceTask", "taskId", taskId));
    }

    /**
     * Logs the reassignment activity
     */
    private void logReassignment(ServiceTask task, String oldTechnicianName,
                                 User newTechnician, String reason) {
        ActivityLog log = new ActivityLog();
        log.setLogId(UUID.randomUUID().toString());
        log.setUser(newTechnician);
        log.setAction("TASK_REASSIGNED");
        log.setDetails(String.format("Task %s reassigned from %s to %s. Reason: %s",
                task.getId(), oldTechnicianName, newTechnician.getFullName(),
                reason != null ? reason : "Not specified"));
        log.setTimestamp(LocalDateTime.now());
        activityLogRepository.save(log);
    }

    /**
     * Logs reassignment that occurred during a task update
     */
    private void logReassignmentDuringUpdate(ServiceTask task, String oldTechnicianName,
                                             User newTechnician, String performedBy) {
        ActivityLog log = new ActivityLog();
        log.setLogId(UUID.randomUUID().toString());
        log.setUser(newTechnician);
        log.setAction("TASK_REASSIGNED");
        log.setDetails(String.format("Task %s reassigned from %s to %s during update by user %s.",
                task.getId(), oldTechnicianName, newTechnician.getFullName(), performedBy));
        log.setTimestamp(LocalDateTime.now());
        activityLogRepository.save(log);
    }

    /**
     * Sends reassignment email notification to the new technician
     *
     * @param task The reassigned task
     * @param newTechnician The technician who received the reassignment
     */
    private void sendReassignmentEmail(ServiceTask task, User newTechnician) {
        try {
            String email = newTechnician.getEmail();

            if (email == null || email.isBlank()) {
                logger.warn("Cannot send reassignment email - technician {} has no email address",
                        newTechnician.getId());
                return;
            }

            String subject = emailTemplateBuilder.buildTaskAssignmentSubject(task, true);
            String body = emailTemplateBuilder.buildTaskAssignmentBody(task, newTechnician, true);

            emailService.sendEmail(email, "Aftercare App", subject, body);

            logger.info("Task reassignment email sent successfully to: {}", email);
        } catch (Exception e) {
            logger.error("Failed to send task reassignment email to technician: {}",
                    newTechnician.getId(), e);
            // Don't throw exception - email failure shouldn't break reassignment
        }
    }

    /**
     * Result object containing reassignment details
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ReassignmentResult {
        private ServiceTask task;
        private String oldTechnicianId;
        private String oldTechnicianName;
        private String newTechnicianId;
        private String newTechnicianName;
        private String reason;
        private boolean wasReassigned;
        private boolean emailSent;
    }
}
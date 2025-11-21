package com.bunsen.api.aftercare.service.helper;

import com.bunsen.api.aftercare.model.ServiceTask;
import com.bunsen.api.aftercare.model.User;
import org.springframework.stereotype.Component;

/**
 * Component for building email templates
 * Centralizes email content generation logic
 */
@Component
public class EmailTemplateBuilder {

    public String buildTaskAssignmentSubject(ServiceTask task, boolean isReassignment) {
        String action = isReassignment ? "New Task Reassigned" : "New Task Assigned";
        return action + " - " + task.getIssueType();
    }

    public String buildTaskAssignmentBody(ServiceTask task, User technician, boolean isReassignment) {
        StringBuilder body = new StringBuilder();

        body.append("Hello ").append(technician.getFullName()).append(",\n\n");

        if (isReassignment) {
            body.append("A task has been reassigned to you.\n\n");
        } else {
            body.append("A new task has been assigned to you.\n\n");
        }

        body.append("Task Details:\n");
        body.append("----------------------------------\n");
        appendTaskDetails(body, task);
        body.append("-----------------------------------\n\n");

        body.append("Please log in to the system to view full task details and update the status.\n\n");
        body.append("If you have any questions, please contact your supervisor.\n\n");

        return body.toString();
    }

    private void appendTaskDetails(StringBuilder body, ServiceTask task) {
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
    }
}
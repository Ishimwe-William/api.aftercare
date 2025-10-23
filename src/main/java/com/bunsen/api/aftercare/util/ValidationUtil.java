package com.bunsen.api.aftercare.util;

import com.bunsen.api.aftercare.exception.ValidationException;
import com.bunsen.api.aftercare.model.ServiceTask;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Centralized validation utility to avoid repetitive validation logic
 */
@Component
public class ValidationUtil {

    /**
     * Validate required string field
     */
    public void validateRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " is required");
        }
    }

    /**
     * Validate email format
     */
    public void validateEmail(String email) {
        if (email != null && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new ValidationException("Invalid email format");
        }
    }

    /**
     * Validate phone number format
     */
    public void validatePhoneNumber(String phone) {
        if (phone != null && !phone.matches("^\\+?[0-9]{10,15}$")) {
            throw new ValidationException("Invalid phone number format");
        }
    }

    /**
     * Validate positive number
     */
    public void validatePositive(Number value, String fieldName) {
        if (value == null || value.doubleValue() <= 0) {
            throw new ValidationException(fieldName + " must be positive");
        }
    }

    /**
     * Validate non-negative number
     */
    public void validateNonNegative(Number value, String fieldName) {
        if (value == null || value.doubleValue() < 0) {
            throw new ValidationException(fieldName + " cannot be negative");
        }
    }

    /**
     * Validate date range
     */
    public void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new ValidationException("Start date and end date are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new ValidationException("Start date must be before end date");
        }
    }

    /**
     * Validate task status transition
     */
    public void validateStatusTransition(ServiceTask.TaskStatus current, ServiceTask.TaskStatus target) {
        switch (target) {
            case IN_PROGRESS:
                if (current != ServiceTask.TaskStatus.PENDING && current != ServiceTask.TaskStatus.PAUSED) {
                    throw new ValidationException(
                            String.format("Cannot transition from %s to IN_PROGRESS", current));
                }
                break;
            case PAUSED:
                if (current != ServiceTask.TaskStatus.IN_PROGRESS) {
                    throw new ValidationException(
                            String.format("Cannot transition from %s to PAUSED", current));
                }
                break;
            case COMPLETED:
                if (current != ServiceTask.TaskStatus.IN_PROGRESS) {
                    throw new ValidationException(
                            String.format("Cannot transition from %s to COMPLETED", current));
                }
                break;
            case PENDING:
                throw new ValidationException("Cannot transition back to PENDING status");
        }
    }

    /**
     * Validate QR code format
     */
    public void validateQrCode(String qrCode) {
        if (qrCode == null || qrCode.trim().isEmpty()) {
            throw new ValidationException("QR code is required");
        }
        // Add additional QR code format validation if needed
    }

    /**
     * Validate stock quantity
     */
    public void validateStockQuantity(int available, int required, String partName) {
        if (available < required) {
            throw new ValidationException(
                    String.format("Insufficient stock for %s. Available: %d, Required: %d",
                            partName, available, required));
        }
    }
}
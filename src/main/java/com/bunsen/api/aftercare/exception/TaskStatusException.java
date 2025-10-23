package com.bunsen.api.aftercare.exception;

import org.springframework.http.HttpStatus;

public class TaskStatusException extends ApiException {

    private static final String REASON = "TASK_STATUS_MISMATCH";

    public TaskStatusException(String currentStatus, String attemptedAction) {
        super(HttpStatus.BAD_REQUEST, REASON,
                String.format("Cannot perform action '%s'. Task is currently in status '%s'.", attemptedAction, currentStatus));
    }
}
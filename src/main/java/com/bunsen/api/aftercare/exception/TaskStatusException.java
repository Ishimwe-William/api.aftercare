package com.bunsen.api.aftercare.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class TaskStatusException extends RuntimeException {
    public TaskStatusException(String currentStatus, String attemptedAction) {
        super(String.format("Invalid action '%s' for task in status '%s'", attemptedAction, currentStatus));
    }
}

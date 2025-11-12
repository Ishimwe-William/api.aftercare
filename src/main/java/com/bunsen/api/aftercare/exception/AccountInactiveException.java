package com.bunsen.api.aftercare.exception;

import org.springframework.http.HttpStatus;

public class AccountInactiveException extends ApiException {

    private static final String REASON = "ACCOUNT_INACTIVE";

    public AccountInactiveException(String message) {
        super(HttpStatus.FORBIDDEN, REASON, message);
    }

    public AccountInactiveException() {
        super(HttpStatus.FORBIDDEN, REASON,
                "Your account has been deactivated. Please contact support to reactivate it.");
    }
}
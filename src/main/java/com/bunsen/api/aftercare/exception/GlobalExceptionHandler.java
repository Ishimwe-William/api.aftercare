package com.bunsen.api.aftercare.exception;

import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import com.bunsen.api.aftercare.service.EmailService;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private final EmailService emailService;

    @Value("${email.sender.username}")
    private String adminEmail;

    public GlobalExceptionHandler(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Centralized handler for all custom API exceptions (inheriting from ApiException).
     * This provides a consistent and user-friendly error response format.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> handleApiException(ApiException ex, WebRequest request) {
        HttpStatus status = ex.getStatus();
        Map<String, Object> body = createErrorBody(status, ex.getErrorReason(), ex.getMessage(), request);
        return new ResponseEntity<>(body, status);
    }

    /**
     * Fallback handler for all other unhandled exceptions.
     * Provides a generic, internal server error message for security and user-friendliness.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGlobalException(Exception ex, WebRequest request) {
        // Log the full exception stack trace internally for debugging
        ex.printStackTrace();

        sendAdminNotificationEmail(ex, request);

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        Map<String, Object> body = createErrorBody(
                status,
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred. The system administrators have been notified.",
                request
        );
        return new ResponseEntity<>(body, status);
    }

    /**
     * Helper method to build a consistent error response body.
     */
    private Map<String, Object> createErrorBody(HttpStatus status, String errorReason, String message, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        // Use the custom reason from the exception for modular, distinct error identification
        body.put("error", errorReason);
        // User-friendly message
        body.put("message", message);
        body.put("path", request.getDescription(false).replace("uri=", ""));
        return body;
    }

    // Handler for Authentication failures (Token is invalid, missing, or credentials are bad)
    @ExceptionHandler({AuthenticationException.class, UnauthorizedException.class})
    public ResponseEntity<?> handleAuthenticationException(RuntimeException ex, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("error", "Unauthorized");
        body.put("message", "Authentication Failed: " + ex.getMessage());
        body.put("path", request.getDescription(false));
        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handler for Authorization failures (User is authenticated but lacks required role).
     * Catches both the MVC AccessDeniedException and the newer AuthorizationDeniedException.
     */
    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<?> handleAuthorizationDenied(RuntimeException ex, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", "Forbidden");
        body.put("message", "Access Denied. You do not have the required permissions.");
        body.put("path", request.getDescription(false));
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    /**
     * Prepares and sends the critical error notification email to the administrator.
     */
    private void sendAdminNotificationEmail(Exception ex, WebRequest request) {
        // Check if admin email is configured
        if (adminEmail == null || adminEmail.trim().isEmpty()) {
            System.err.println("Admin email not configured. Skipping error notification email.");
            return;
        }

        String timestamp = LocalDateTime.now().toString();
        String path = request.getDescription(false).replace("uri=", "");

        // Capture stack trace for detailed logging
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        String stackTrace = sw.toString();

        String subject = String.format("CRITICAL API ERROR (500) - %s", ex.getClass().getSimpleName());

        String body = String.format(
                "Time: %s\n" +
                        "Path: %s\n" +
                        "Error Type: %s\n" +
                        "Message: %s\n" +
                        "==================================\n" +
                        "Stack Trace:\n%s",
                timestamp,
                path,
                ex.getClass().getName(),
                ex.getMessage(),
                stackTrace
        );

        try {
            // Note: EmailService.sendEmail method signature is sendEmail(mailTo, senderName, subject, body)
            emailService.sendEmail(adminEmail, "Aftercare API System", subject, body);
        } catch (MessagingException mailEx) {
            // Log if the email sending itself failed, but do not stop the main request handling
            System.err.println("Failed to send 500 error notification email: " + mailEx.getMessage());
        }
    }
}
package com.bunsen.api.aftercare.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class EmailService {
    private final JavaMailSender emailSender;
    private final Logger LOG = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    public EmailService(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    /**
     * Synchronous email sending - throws exception on failure
     */
    public void sendEmail(String mailTo, String senderName, String subject, String body) throws MessagingException {
        if (mailTo == null || mailTo.isBlank()) {
            throw new IllegalArgumentException("Recipient email cannot be null or empty");
        }
        String htmlBody = createHtmlBody(senderName, body);
        sendEmailInternal(mailTo, subject, htmlBody);
    }

    /**
     * Asynchronous email sending - logs errors but doesn't throw
     * Use this for non-critical notifications
     */
    @Async
    public CompletableFuture<Boolean> sendEmailAsync(String mailTo, String senderName, String subject, String body) {
        try {
            if (mailTo == null || mailTo.isBlank()) {
                LOG.warn("Cannot send email - recipient address is null or empty");
                return CompletableFuture.completedFuture(false);
            }

            String htmlBody = createHtmlBody(senderName, body);
            sendEmailInternal(mailTo, subject, htmlBody);
            LOG.info("Async email sent successfully to {}", mailTo);
            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            LOG.error("Failed to send async email to {}: {}", mailTo, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Safe email sending - never throws exceptions
     * Returns true if sent successfully, false otherwise
     */
    public boolean sendEmailSafe(String mailTo, String senderName, String subject, String body) {
        try {
            sendEmail(mailTo, senderName, subject, body);
            return true;
        } catch (Exception e) {
            LOG.error("Failed to send email to {}: {}", mailTo, e.getMessage());
            return false;
        }
    }

    private String createHtmlBody(String senderName, String body) {
        return "<html><body>" +
                "<p>Dear User,</p>" +
                "<pre>" + body + "</pre>" +
                "<p>Best regards,</p>" +
                "<p>" + senderName + "</p>" +
                "</body></html>";
    }

    private void sendEmailInternal(String mailTo, String subject, String mailBody) throws MessagingException {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(mailTo);
            helper.setSubject(subject);
            helper.setText(mailBody, true);
            emailSender.send(message);
            LOG.info("Email sent successfully to {}", mailTo);
        } catch (MessagingException e) {
            LOG.error("MessagingException while sending email to {}: {}", mailTo, e.getMessage());
            throw e;
        } catch (Exception e) {
            LOG.error("Unexpected error while sending email to {}: {}", mailTo, e.getMessage(), e);
            throw new MessagingException("Failed to send email", e);
        }
    }
}
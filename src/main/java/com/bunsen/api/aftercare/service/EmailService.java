package com.bunsen.api.aftercare.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender emailSender;
    private final Logger LOG = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    public EmailService(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void sendEmail(String mailTo, String senderName, String subject, String body) throws MessagingException {
        if (mailTo == null || mailTo.isBlank()) {
            throw new IllegalArgumentException("Recipient email cannot be null or empty");
        }
        String htmlBody = createHtmlBody(senderName, body);
        sendEmail(mailTo, subject, htmlBody);
    }

    private String createHtmlBody(String senderName, String body) {
        return "<html><body>" +
                "<p>Dear User,</p>" +
                "<pre>" + body + "</pre>" +
                "<p>Best regards,</p>" +
                "<p>" + senderName + "</p>" +
                "</body></html>";
    }

    private void sendEmail(String mailTo, String subject, String mailBody) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(mailTo);
        helper.setSubject(subject);
        helper.setText(mailBody, true);
        emailSender.send(message);
        LOG.info("Email sent to {}", mailTo);
    }
}
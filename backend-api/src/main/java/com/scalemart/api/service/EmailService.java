package com.scalemart.api.service;

import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final boolean mailEnabled;
    private final String fromAddress;
    private final String appBaseUrl;

    public EmailService(
        JavaMailSender mailSender,
        @Value("${app.mail.enabled:false}") boolean mailEnabled,
        @Value("${app.mail.from:no-reply@scalemart.dev}") String fromAddress,
        @Value("${app.mail.base-url:http://localhost:8080}") String appBaseUrl) {
        this.mailSender = mailSender;
        this.mailEnabled = mailEnabled;
        this.fromAddress = fromAddress;
        this.appBaseUrl = appBaseUrl;
    }

    public boolean sendVerificationEmail(String toEmail, String username, String token) {
        String encodedUsername = UriUtils.encode(username, StandardCharsets.UTF_8);
        String encodedToken = UriUtils.encode(token, StandardCharsets.UTF_8);
        String verificationLink = appBaseUrl + "/?verify=1&username=" + encodedUsername + "&token=" + encodedToken;

        String subject = "ScaleMart email verification";
        String body = """
            Hi %s,

            Welcome to ScaleMart.

            Verify your email using this link:
            %s

            If needed, manual verification token:
            %s

            This token expires in 24 hours.
            """.formatted(username, verificationLink, token);

        return sendSimpleEmail(toEmail, subject, body);
    }

    public boolean sendPasswordResetEmail(String toEmail, String username, String token) {
        String encodedUsername = UriUtils.encode(username, StandardCharsets.UTF_8);
        String encodedToken = UriUtils.encode(token, StandardCharsets.UTF_8);
        String resetLink = appBaseUrl + "/?reset=1&username=" + encodedUsername + "&token=" + encodedToken;

        String subject = "ScaleMart password reset";
        String body = """
            Hi %s,

            We received a password reset request.

            Reset link:
            %s

            If needed, manual reset token:
            %s

            This token expires in 30 minutes.
            """.formatted(username, resetLink, token);

        return sendSimpleEmail(toEmail, subject, body);
    }

    private boolean sendSimpleEmail(String toEmail, String subject, String body) {
        if (!mailEnabled) {
            log.info("SMTP delivery disabled. Skipping mail to={} subject={}", toEmail, subject);
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            return true;
        } catch (MailException exception) {
            log.error("Failed to send SMTP email to={} subject={}", toEmail, subject, exception);
            return false;
        }
    }
}

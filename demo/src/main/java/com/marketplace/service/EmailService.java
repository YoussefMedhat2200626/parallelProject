package com.marketplace.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Email service for sending OTP verification codes.
 * Sends emails asynchronously so the user doesn't wait for SMTP completion.
 */
@Service
public class EmailService {

    private static final Logger LOG = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send an OTP verification code to the user's email address.
     */
    @Async
    public void sendOtpEmail(String toEmail, String otpCode, String purpose) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Marketplace - Your Verification Code");
            message.setText(buildOtpEmailBody(otpCode, purpose));

            mailSender.send(message);
            LOG.info("OTP email sent successfully to {} for purpose: {}", toEmail, purpose);
        } catch (Exception e) {
            LOG.error("Failed to send OTP email to {}", toEmail, e);
        }
    }

    private String buildOtpEmailBody(String otpCode, String purpose) {
        String action;
        switch (purpose) {
            case "ACCOUNT_CREATE" -> action = "complete your account registration";
            case "PURCHASE" -> action = "confirm your purchase";
            case "LOGIN" -> action = "log in to your account";
            default -> action = "verify your identity";
        }

        return String.format("""
                ========================================
                   Marketplace Verification Code
                ========================================

                Your verification code is: %s

                Use this code to %s.
                This code expires in 5 minutes.

                If you did not request this code, please
                ignore this email.

                — Marketplace Team
                ========================================
                """, otpCode, action);
    }
}

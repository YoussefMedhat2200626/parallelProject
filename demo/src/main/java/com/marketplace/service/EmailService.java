package com.marketplace.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

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
    @SuppressWarnings("null")
    public void sendOtpEmail(String toEmail, String otpCode, String purpose) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, 
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, 
                StandardCharsets.UTF_8.name());

            helper.setTo(toEmail);
            helper.setSubject("Marketplace - Your Verification Code [" + otpCode + "]");
            helper.setText(buildOtpEmailHtml(otpCode, purpose), true);
            helper.setFrom("noreply.marketplace.verify@gmail.com", "Marketplace Verification");

            mailSender.send(message);
            LOG.info("Premium HTML OTP email sent successfully to {} for purpose: {}", toEmail, purpose);
        } catch (Exception e) {
            LOG.error("Failed to send premium OTP email to {}", toEmail, e);
        }
    }

    private String buildOtpEmailHtml(String otpCode, String purpose) {
        String action;
        switch (purpose) {
            case "ACCOUNT_CREATE" -> action = "complete your account registration";
            case "PURCHASE" -> action = "confirm your purchase";
            case "LOGIN" -> action = "log in to your account";
            default -> action = "verify your identity";
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f7f9; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 40px auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 10px 30px rgba(0,0,0,0.05); }
                    .header { background: linear-gradient(135deg, #1a237e 0%%, #0d47a1 100%%); padding: 40px 20px; text-align: center; color: white; }
                    .header h1 { margin: 0; font-size: 28px; letter-spacing: 1px; }
                    .content { padding: 40px; text-align: center; color: #37474f; line-height: 1.6; }
                    .otp-card { background: #f8f9fa; border: 2px dashed #0d47a1; border-radius: 8px; padding: 20px; margin: 30px 0; display: inline-block; }
                    .otp-code { font-size: 36px; font-weight: bold; color: #0d47a1; letter-spacing: 8px; margin: 0; }
                    .footer { padding: 20px; text-align: center; font-size: 12px; color: #90a4ae; background: #fafafa; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #0d47a1; color: white; text-decoration: none; border-radius: 6px; font-weight: 500; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>MARKETPLACE</h1>
                    </div>
                    <div class="content">
                        <h2 style="color: #1a237e;">Verification Required</h2>
                        <p>Hello,</p>
                        <p>Use the code below to <strong>%s</strong>. This code is valid for 5 minutes.</p>
                        
                        <div class="otp-card">
                            <p style="margin: 0 0 10px 0; font-size: 14px; color: #546e7a; text-transform: uppercase;">Your Security Code</p>
                            <div class="otp-code">%s</div>
                        </div>
                        
                        <p style="font-size: 14px; color: #78909c;">If you did not request this code, please ignore this email or contact support if you have concerns.</p>
                    </div>
                    <div class="footer">
                        &copy; 2026 Marketplace Distributed Systems Project<br>
                        Ain Shams University - Faculty of Engineering
                    </div>
                </div>
            </body>
            </html>
            """.formatted(action, otpCode);
    }
}

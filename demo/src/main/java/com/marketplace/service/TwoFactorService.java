package com.marketplace.service;

import com.marketplace.entity.OtpCode;
import com.marketplace.entity.OtpCode.OtpPurpose;
import com.marketplace.repository.OtpRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Two-Factor Authentication service.
 * Generates 6-digit OTP codes with configurable TTL.
 * Codes are stored in MariaDB and validated once (single-use).
 * 
 * NOTE: In production, the OTP would be sent via email/SMS.
 * For this demo, the OTP is logged to console and shown in the UI.
 */
@Service
public class TwoFactorService {

    private static final Logger LOG = LoggerFactory.getLogger(TwoFactorService.class);
    private static final int OTP_LENGTH = 6;
    private static final int DEFAULT_TTL_MINUTES = 5;

    private final OtpRepository otpRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public TwoFactorService(OtpRepository otpRepository) {
        this.otpRepository = otpRepository;
    }

    /**
     * Generate and store a new OTP code for a user.
     * In a real system, this would be sent via email/SMS.
     */
    @Transactional
    public String generateOtp(Long userId, OtpPurpose purpose) {
        String code = String.format("%0" + OTP_LENGTH + "d", secureRandom.nextInt((int) Math.pow(10, OTP_LENGTH)));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(DEFAULT_TTL_MINUTES);

        OtpCode otp = new OtpCode(userId, code, purpose, expiresAt);
        otpRepository.save(otp);

        // In production: send via email/SMS. For demo: log to console.
        LOG.info("=== 2FA OTP for user {} ({}): {} === (expires at {})", userId, purpose, code, expiresAt);
        return code;
    }

    /**
     * Validate an OTP code. Single-use: once validated, it cannot be reused.
     */
    @Transactional
    public boolean validateOtp(Long userId, String code, OtpPurpose purpose) {
        Optional<OtpCode> otpOpt = otpRepository.findValidOtp(userId, code, purpose);

        if (otpOpt.isPresent()) {
            OtpCode otp = otpOpt.get();
            otp.setUsed(true);
            otpRepository.save(otp);
            LOG.info("OTP validated successfully for user {} ({})", userId, purpose);
            return true;
        }

        LOG.warn("Invalid or expired OTP for user {} ({})", userId, purpose);
        return false;
    }
}

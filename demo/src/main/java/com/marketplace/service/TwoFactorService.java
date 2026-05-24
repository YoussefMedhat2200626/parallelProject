package com.marketplace.service;

import com.marketplace.entity.OtpCode;
import com.marketplace.entity.OtpCode.OtpPurpose;
import com.marketplace.repository.OtpRepository;
import com.marketplace.repository.UserRepository;
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
 * Codes are stored in MariaDB, sent via email, and validated once (single-use).
 */
@Service
public class TwoFactorService {

    private static final Logger LOG = LoggerFactory.getLogger(TwoFactorService.class);
    private static final int OTP_LENGTH = 6;
    private static final int DEFAULT_TTL_MINUTES = 5;

    private final OtpRepository otpRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public TwoFactorService(OtpRepository otpRepository, UserRepository userRepository,
                            EmailService emailService) {
        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /**
     * Generate, store, and email a new OTP code for a user.
     */
    @Transactional
    public String generateOtp(Long userId, OtpPurpose purpose) {
        String code = String.format("%0" + OTP_LENGTH + "d", secureRandom.nextInt((int) Math.pow(10, OTP_LENGTH)));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(DEFAULT_TTL_MINUTES);

        OtpCode otp = new OtpCode(userId, code, purpose, expiresAt);
        otpRepository.save(otp);

        LOG.info("2FA OTP generated for user {} ({}): expires at {}", userId, purpose, expiresAt);

        // Send OTP via email
        if (userId != null) {
            userRepository.findById(userId).ifPresent(user -> {
            String email = user.getEmail();
            if (email != null && !email.isBlank()) {
                emailService.sendOtpEmail(email, code, purpose.name());
                LOG.info("OTP email dispatched to {} for user {}", email, userId);
            } else {
                LOG.warn("No email address found for user {}, OTP not sent via email", userId);
            }
            });
        }

        return code;
    }

    /**
     * Generate, store, and email a new OTP code — using email directly
     * (for registration before the user is fully saved).
     */
    @Transactional
    public String generateOtpWithEmail(Long userId, String email, OtpPurpose purpose) {
        String code = String.format("%0" + OTP_LENGTH + "d", secureRandom.nextInt((int) Math.pow(10, OTP_LENGTH)));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(DEFAULT_TTL_MINUTES);

        OtpCode otp = new OtpCode(userId, code, purpose, expiresAt);
        otpRepository.save(otp);

        LOG.info("2FA OTP generated for user {} ({}): expires at {}", userId, purpose, expiresAt);

        // Send OTP via email directly
        if (email != null && !email.isBlank()) {
            emailService.sendOtpEmail(email, code, purpose.name());
            LOG.info("OTP email dispatched to {} for user {}", email, userId);
        }

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

    /**
     * Checks if a user is verified.
     * A user is verified if they have successfully used an ACCOUNT_CREATE OTP,
     * or if they never had one generated (e.g. legacy or manual accounts).
     */
    public boolean isUserVerified(Long userId) {
        boolean hasAny = otpRepository.existsByUserIdAndPurpose(userId, OtpPurpose.ACCOUNT_CREATE);
        boolean hasVerified = otpRepository.existsByUserIdAndPurposeAndUsedTrue(userId, OtpPurpose.ACCOUNT_CREATE);
        return !hasAny || hasVerified;
    }
}

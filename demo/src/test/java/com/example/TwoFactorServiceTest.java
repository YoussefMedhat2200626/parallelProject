package com.example;

import com.example.service.TwoFactorService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TwoFactorServiceTest {

    @Test
    void otpCodeCanBeUsedOnce() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));
        TwoFactorService service = new TwoFactorService(now::get, new Random(7), Duration.ofMinutes(2));

        String code = service.issueCode("alice");
        assertTrue(service.validateCode("alice", code));
        assertFalse(service.validateCode("alice", code));
    }

    @Test
    void otpCodeExpires() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));
        TwoFactorService service = new TwoFactorService(now::get, new Random(17), Duration.ofSeconds(10));

        String code = service.issueCode("alice");
        now.set(now.get().plusSeconds(11));

        assertFalse(service.validateCode("alice", code));
    }
}

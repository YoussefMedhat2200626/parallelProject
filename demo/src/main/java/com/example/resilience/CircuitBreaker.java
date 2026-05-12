package com.example.resilience;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

public class CircuitBreaker {
    private final int failureThreshold;
    private final Duration openDuration;
    private final Clock clock;

    private int consecutiveFailures = 0;
    private Instant openedAt;

    public CircuitBreaker(int failureThreshold, Duration openDuration, Clock clock) {
        if (failureThreshold < 1) {
            throw new IllegalArgumentException("failureThreshold must be >= 1");
        }
        this.failureThreshold = failureThreshold;
        this.openDuration = Objects.requireNonNull(openDuration, "openDuration must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public synchronized <T> T execute(Supplier<T> action) {
        Objects.requireNonNull(action, "action must not be null");
        if (isOpen()) {
            throw new CircuitOpenException("Circuit is open. Requests are temporarily blocked.");
        }

        try {
            T result = action.get();
            consecutiveFailures = 0;
            openedAt = null;
            return result;
        } catch (RuntimeException ex) {
            consecutiveFailures++;
            if (consecutiveFailures >= failureThreshold) {
                openedAt = clock.instant();
            }
            throw ex;
        }
    }

    private boolean isOpen() {
        if (openedAt == null) {
            return false;
        }
        Instant now = clock.instant();
        if (now.isBefore(openedAt.plus(openDuration))) {
            return true;
        }
        openedAt = null;
        consecutiveFailures = 0;
        return false;
    }
}

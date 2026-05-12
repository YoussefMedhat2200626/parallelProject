package com.example.resilience;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class RetryExecutor {
    public <T> T execute(
            Supplier<T> action,
            int maxAttempts,
            Duration backoff,
            Predicate<RuntimeException> retriable
    ) {
        Objects.requireNonNull(action, "action must not be null");
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }

        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (RuntimeException ex) {
                last = ex;
                if (attempt == maxAttempts || !retriable.test(ex)) {
                    throw ex;
                }
                sleep(backoff);
            }
        }
        if (last != null) {
            throw last;
        }
        throw new IllegalStateException("Unexpected state in RetryExecutor");
    }

    private void sleep(Duration backoff) {
        if (backoff == null || backoff.isZero() || backoff.isNegative()) {
            return;
        }
        try {
            Thread.sleep(backoff.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Retry interrupted", ex);
        }
    }
}

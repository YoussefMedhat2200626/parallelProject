package com.example.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class TwoFactorService {
    private final Supplier<Instant> nowSupplier;
    private final Random random;
    private final Duration defaultTtl;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Instant>> challengesByAccount = new ConcurrentHashMap<>();

    public TwoFactorService(Supplier<Instant> nowSupplier, Random random, Duration defaultTtl) {
        this.nowSupplier = Objects.requireNonNull(nowSupplier, "nowSupplier must not be null");
        this.random = Objects.requireNonNull(random, "random must not be null");
        this.defaultTtl = Objects.requireNonNull(defaultTtl, "defaultTtl must not be null");
    }

    public String issueCode(String accountId) {
        return issueCode(accountId, defaultTtl);
    }

    public String issueCode(String accountId, Duration ttl) {
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("accountId must not be blank");
        }
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        String code = String.format("%06d", random.nextInt(1_000_000));
        challengesByAccount
                .computeIfAbsent(accountId, ignored -> new ConcurrentHashMap<>())
                .put(code, nowSupplier.get().plus(ttl));
        return code;
    }

    public boolean validateCode(String accountId, String code) {
        ConcurrentHashMap<String, Instant> accountChallenges = challengesByAccount.get(accountId);
        if (accountChallenges == null || code == null || code.isBlank()) {
            return false;
        }

        Instant expiresAt = accountChallenges.get(code);
        if (expiresAt == null) {
            return false;
        }

        Instant now = nowSupplier.get();
        if (now.isAfter(expiresAt)) {
            accountChallenges.remove(code);
            return false;
        }

        accountChallenges.remove(code);
        if (accountChallenges.isEmpty()) {
            challengesByAccount.remove(accountId, accountChallenges);
        }
        return true;
    }
}

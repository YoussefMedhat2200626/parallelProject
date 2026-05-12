package com.example.service;

import com.example.domain.TradeRequest;
import com.example.resilience.RetryExecutor;
import com.example.resilience.TransientServiceException;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

public class MarketplaceService {
    private final WalletService walletService;
    private final TwoFactorService twoFactorService;
    private final MarketplaceClient marketplaceClient;
    private final RetryExecutor retryExecutor;

    public MarketplaceService(
            WalletService walletService,
            TwoFactorService twoFactorService,
            MarketplaceClient marketplaceClient,
            RetryExecutor retryExecutor
    ) {
        this.walletService = Objects.requireNonNull(walletService, "walletService must not be null");
        this.twoFactorService = Objects.requireNonNull(twoFactorService, "twoFactorService must not be null");
        this.marketplaceClient = Objects.requireNonNull(marketplaceClient, "marketplaceClient must not be null");
        this.retryExecutor = Objects.requireNonNull(retryExecutor, "retryExecutor must not be null");
    }

    public String executeTrade(TradeRequest request) {
        validateRequest(request);
        if (!twoFactorService.validateCode(request.fromAccountId(), request.otpCode())) {
            throw new SecurityException("Invalid or expired 2FA code");
        }

        String reference = UUID.randomUUID().toString();
        return retryExecutor.execute(
                () -> {
                    if (!marketplaceClient.confirmTrade(reference)) {
                        throw new TransientServiceException("Marketplace confirmation failed");
                    }
                    return walletService.transfer(
                            request.fromAccountId(),
                            request.toAccountId(),
                            request.amountInCents(),
                            reference
                    );
                },
                3,
                Duration.ofMillis(25),
                ex -> ex instanceof TransientServiceException
        );
    }

    private void validateRequest(TradeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (request.fromAccountId() == null || request.fromAccountId().isBlank()) {
            throw new IllegalArgumentException("fromAccountId must not be blank");
        }
        if (request.toAccountId() == null || request.toAccountId().isBlank()) {
            throw new IllegalArgumentException("toAccountId must not be blank");
        }
        if (request.amountInCents() <= 0) {
            throw new IllegalArgumentException("amountInCents must be > 0");
        }
    }
}

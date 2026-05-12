package com.example.service;

import com.example.domain.GatewayResponse;
import com.example.domain.TradeRequest;
import com.example.resilience.CircuitBreaker;

import java.util.Objects;

public class ApiGatewayService {
    private final WalletService walletService;
    private final MarketplaceService marketplaceService;
    private final CircuitBreaker marketplaceCircuitBreaker;

    public ApiGatewayService(
            WalletService walletService,
            MarketplaceService marketplaceService,
            CircuitBreaker marketplaceCircuitBreaker
    ) {
        this.walletService = Objects.requireNonNull(walletService, "walletService must not be null");
        this.marketplaceService = Objects.requireNonNull(marketplaceService, "marketplaceService must not be null");
        this.marketplaceCircuitBreaker = Objects.requireNonNull(marketplaceCircuitBreaker, "marketplaceCircuitBreaker must not be null");
    }

    public void createWalletAccount(String accountId, long initialBalanceInCents) {
        walletService.createAccount(accountId, initialBalanceInCents);
    }

    public GatewayResponse submitTrade(TradeRequest request) {
        try {
            String ref = marketplaceCircuitBreaker.execute(() -> marketplaceService.executeTrade(request));
            return new GatewayResponse(true, "Trade completed", ref);
        } catch (RuntimeException ex) {
            return new GatewayResponse(false, ex.getMessage(), null);
        }
    }
}

package com.example;

import com.example.domain.GatewayResponse;
import com.example.domain.TradeRequest;
import com.example.resilience.CircuitBreaker;
import com.example.resilience.RetryExecutor;
import com.example.service.ApiGatewayService;
import com.example.service.LedgerService;
import com.example.service.MarketplaceService;
import com.example.service.TwoFactorService;
import com.example.service.UnstableMarketplaceClient;
import com.example.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        LedgerService ledgerService = new LedgerService(Clock.systemUTC());
        WalletService walletService = new WalletService(ledgerService);
        TwoFactorService twoFactorService = new TwoFactorService(Instant::now, new Random(), Duration.ofMinutes(5));
        MarketplaceService marketplaceService = new MarketplaceService(
                walletService,
                twoFactorService,
                new UnstableMarketplaceClient(4),
                new RetryExecutor()
        );
        ApiGatewayService apiGatewayService = new ApiGatewayService(
                walletService,
                marketplaceService,
                new CircuitBreaker(3, Duration.ofSeconds(2), Clock.systemUTC())
        );

        apiGatewayService.createWalletAccount("alice", 50_000);
        apiGatewayService.createWalletAccount("market", 10_000);

        try (ExecutorService executor = Executors.newFixedThreadPool(6)) {
            List<TradeRequest> requests = java.util.stream.IntStream.range(0, 20)
                    .mapToObj(ignored -> new TradeRequest(
                            "alice",
                            "market",
                            500,
                            twoFactorService.issueCode("alice")
                    ))
                    .toList();

            requests.forEach(request -> executor.submit(() -> logTradeResult(apiGatewayService.submitTrade(request))));
        }

        LOGGER.info("Final balance alice={} cents, market={} cents",
                walletService.getBalance("alice"),
                walletService.getBalance("market"));
        LOGGER.info("Ledger entries recorded={}", ledgerService.getEntriesSnapshot().size());
    }

    private static void logTradeResult(GatewayResponse response) {
        if (response.success()) {
            LOGGER.info("Trade succeeded with reference {}", response.reference());
        } else {
            LOGGER.warn("Trade failed: {}", response.message());
        }
    }
}
package com.example;

import com.example.domain.GatewayResponse;
import com.example.domain.TradeRequest;
import com.example.resilience.CircuitBreaker;
import com.example.resilience.RetryExecutor;
import com.example.service.ApiGatewayService;
import com.example.service.LedgerService;
import com.example.service.MarketplaceService;
import com.example.service.TwoFactorService;
import com.example.service.WalletService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayIntegrationTest {

    @Test
    void completesTradeWhenOtpIsValid() {
        LedgerService ledger = new LedgerService(Clock.systemUTC());
        WalletService wallet = new WalletService(ledger);
        TwoFactorService twoFactor = new TwoFactorService(Instant::now, new Random(3), Duration.ofMinutes(5));
        MarketplaceService marketplace = new MarketplaceService(wallet, twoFactor, reference -> true, new RetryExecutor());
        ApiGatewayService gateway = new ApiGatewayService(
                wallet,
                marketplace,
                new CircuitBreaker(2, Duration.ofSeconds(1), Clock.systemUTC())
        );

        gateway.createWalletAccount("alice", 20_000);
        gateway.createWalletAccount("market", 1_000);

        String otp = twoFactor.issueCode("alice");
        GatewayResponse response = gateway.submitTrade(new TradeRequest("alice", "market", 1_000, otp));

        assertTrue(response.success());
        assertEquals(19_000, wallet.getBalance("alice"));
        assertEquals(2_000, wallet.getBalance("market"));
        assertEquals(4, ledger.getEntriesSnapshot().size());
    }

    @Test
    void rejectsTradeWhenOtpInvalid() {
        LedgerService ledger = new LedgerService(Clock.systemUTC());
        WalletService wallet = new WalletService(ledger);
        TwoFactorService twoFactor = new TwoFactorService(Instant::now, new Random(5), Duration.ofMinutes(5));
        MarketplaceService marketplace = new MarketplaceService(wallet, twoFactor, reference -> true, new RetryExecutor());
        ApiGatewayService gateway = new ApiGatewayService(
                wallet,
                marketplace,
                new CircuitBreaker(2, Duration.ofSeconds(1), Clock.systemUTC())
        );

        gateway.createWalletAccount("alice", 20_000);
        gateway.createWalletAccount("market", 1_000);

        GatewayResponse response = gateway.submitTrade(new TradeRequest("alice", "market", 1_000, "000000"));

        assertFalse(response.success());
        assertEquals(20_000, wallet.getBalance("alice"));
        assertEquals(1_000, wallet.getBalance("market"));
    }
}

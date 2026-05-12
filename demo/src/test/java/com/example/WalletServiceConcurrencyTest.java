package com.example;

import com.example.service.LedgerService;
import com.example.service.WalletService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WalletServiceConcurrencyTest {

    @Test
    void handlesParallelTransfersConsistently() throws Exception {
        LedgerService ledger = new LedgerService(Clock.systemUTC());
        WalletService wallet = new WalletService(ledger);
        wallet.createAccount("source", 500_000);
        wallet.createAccount("destination", 100_000);

        List<Callable<String>> tasks = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            tasks.add(() -> wallet.transfer("source", "destination", 500, "load-test"));
        }

        try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
            List<Future<String>> futures = executor.invokeAll(tasks);
            for (Future<String> future : futures) {
                future.get();
            }
        }

        assertEquals(400_000, wallet.getBalance("source"));
        assertEquals(200_000, wallet.getBalance("destination"));
        assertEquals(402, ledger.getEntriesSnapshot().size());
    }
}

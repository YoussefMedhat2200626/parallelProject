package com.example.service;

import com.example.resilience.TransientServiceException;

import java.util.concurrent.atomic.AtomicInteger;

public class UnstableMarketplaceClient implements MarketplaceClient {
    private final int failEvery;
    private final AtomicInteger calls = new AtomicInteger(0);

    public UnstableMarketplaceClient(int failEvery) {
        this.failEvery = failEvery;
    }

    @Override
    public boolean confirmTrade(String reference) {
        if (failEvery > 0 && calls.incrementAndGet() % failEvery == 0) {
            throw new TransientServiceException("Marketplace network timeout for reference: " + reference);
        }
        return true;
    }
}

package com.example.service;

import com.example.domain.LedgerEntry;
import com.example.domain.TransactionType;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class LedgerService {
    private final AtomicLong sequence = new AtomicLong(1);
    private final CopyOnWriteArrayList<LedgerEntry> entries = new CopyOnWriteArrayList<>();
    private final Clock clock;

    public LedgerService(Clock clock) {
        this.clock = clock;
    }

    public LedgerEntry append(String accountId, long amountInCents, TransactionType type, String reference) {
        LedgerEntry entry = new LedgerEntry(
                sequence.getAndIncrement(),
                clock.instant(),
                accountId,
                amountInCents,
                type,
                reference
        );
        entries.add(entry);
        return entry;
    }

    public List<LedgerEntry> getEntriesSnapshot() {
        return List.copyOf(entries);
    }

    public long computeBalance(String accountId) {
        return entries.stream()
                .filter(entry -> entry.accountId().equals(accountId))
                .mapToLong(LedgerEntry::amountInCents)
                .sum();
    }
}

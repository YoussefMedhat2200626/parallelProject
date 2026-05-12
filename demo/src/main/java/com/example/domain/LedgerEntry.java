package com.example.domain;

import java.time.Instant;

public record LedgerEntry(
        long id,
        Instant timestamp,
        String accountId,
        long amountInCents,
        TransactionType type,
        String reference
) {
}

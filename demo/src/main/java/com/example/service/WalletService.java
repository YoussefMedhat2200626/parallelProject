package com.example.service;

import com.example.domain.TransactionType;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class WalletService {
    private final ConcurrentHashMap<String, AtomicLong> balances = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> accountLocks = new ConcurrentHashMap<>();
    private final LedgerService ledgerService;

    public WalletService(LedgerService ledgerService) {
        this.ledgerService = Objects.requireNonNull(ledgerService, "ledgerService must not be null");
    }

    public void createAccount(String accountId, long initialBalanceInCents) {
        validateAmount(initialBalanceInCents);
        AtomicLong previous = balances.putIfAbsent(accountId, new AtomicLong(initialBalanceInCents));
        if (previous != null) {
            throw new IllegalArgumentException("Account already exists: " + accountId);
        }
        if (initialBalanceInCents > 0) {
            ledgerService.append(accountId, initialBalanceInCents, TransactionType.DEPOSIT, "initial-funding");
        }
    }

    public long getBalance(String accountId) {
        AtomicLong value = balances.get(accountId);
        if (value == null) {
            throw new IllegalArgumentException("Account not found: " + accountId);
        }
        return value.get();
    }

    public String transfer(String fromAccountId, String toAccountId, long amountInCents, String referenceHint) {
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Transfer accounts must be different");
        }
        validateAmount(amountInCents);
        AtomicLong fromBalance = balances.get(fromAccountId);
        AtomicLong toBalance = balances.get(toAccountId);
        if (fromBalance == null || toBalance == null) {
            throw new IllegalArgumentException("Both accounts must exist before transfer");
        }

        String first = fromAccountId.compareTo(toAccountId) < 0 ? fromAccountId : toAccountId;
        String second = fromAccountId.compareTo(toAccountId) < 0 ? toAccountId : fromAccountId;

        ReentrantLock firstLock = accountLocks.computeIfAbsent(first, ignored -> new ReentrantLock());
        ReentrantLock secondLock = accountLocks.computeIfAbsent(second, ignored -> new ReentrantLock());

        firstLock.lock();
        secondLock.lock();
        try {
            if (fromBalance.get() < amountInCents) {
                throw new IllegalStateException("Insufficient funds for account: " + fromAccountId);
            }
            fromBalance.addAndGet(-amountInCents);
            toBalance.addAndGet(amountInCents);

            String reference = referenceHint == null || referenceHint.isBlank()
                    ? UUID.randomUUID().toString()
                    : referenceHint;
            ledgerService.append(fromAccountId, -amountInCents, TransactionType.MARKETPLACE_TRADE, reference);
            ledgerService.append(toAccountId, amountInCents, TransactionType.MARKETPLACE_TRADE, reference);
            return reference;
        } finally {
            secondLock.unlock();
            firstLock.unlock();
        }
    }

    private void validateAmount(long amountInCents) {
        if (amountInCents <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }
}

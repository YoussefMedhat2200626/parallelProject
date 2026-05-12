package com.marketplace.service;

import com.marketplace.entity.DepositLedger;
import com.marketplace.entity.Wallet;
import com.marketplace.repository.DepositLedgerRepository;
import com.marketplace.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Manages wallet balances, deposits, and fund transfers between users.
 * All amounts are in cents to avoid floating-point issues.
 */
@Service
public class WalletService {

    private static final Logger LOG = LoggerFactory.getLogger(WalletService.class);
    private final WalletRepository walletRepository;
    private final DepositLedgerRepository depositLedgerRepository;

    public WalletService(WalletRepository walletRepository, DepositLedgerRepository depositLedgerRepository) {
        this.walletRepository = walletRepository;
        this.depositLedgerRepository = depositLedgerRepository;
    }

    public Wallet getWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for user: " + userId));
    }

    public Long getBalance(Long userId) {
        return getWallet(userId).getBalanceCents();
    }

    /**
     * Deposit funds into a user's wallet.
     */
    @Transactional
    public DepositLedger deposit(Long userId, Long amountCents) {
        if (amountCents <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }

        int updated = walletRepository.updateBalance(userId, amountCents);
        if (updated == 0) {
            throw new IllegalStateException("Failed to deposit - wallet not found for user: " + userId);
        }

        String ref = "DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        DepositLedger ledger = new DepositLedger(userId, amountCents, "MANUAL", ref);
        ledger = depositLedgerRepository.save(ledger);

        LOG.info("Deposited {} cents to user {} (ref={})", amountCents, userId, ref);
        return ledger;
    }

    /**
     * Transfer funds from buyer to seller atomically.
     */
    @Transactional
    public String transfer(Long fromUserId, Long toUserId, Long amountCents) {
        if (fromUserId.equals(toUserId)) {
            throw new IllegalArgumentException("Cannot transfer to yourself");
        }
        if (amountCents <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        // Debit buyer
        int debited = walletRepository.updateBalance(fromUserId, -amountCents);
        if (debited == 0) {
            throw new IllegalStateException("Insufficient funds or wallet not found for user: " + fromUserId);
        }

        // Credit seller
        int credited = walletRepository.updateBalance(toUserId, amountCents);
        if (credited == 0) {
            // Rollback debit
            walletRepository.updateBalance(fromUserId, amountCents);
            throw new IllegalStateException("Seller wallet not found: " + toUserId);
        }

        String ref = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LOG.info("Transferred {} cents from user {} to user {} (ref={})", amountCents, fromUserId, toUserId, ref);
        return ref;
    }

    public List<DepositLedger> getDepositHistory(Long userId) {
        return depositLedgerRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}

package com.marketplace.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Long walletId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "balance_cents", nullable = false)
    private Long balanceCents = 0L;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Wallet() {}

    public Wallet(Long userId, Long balanceCents) {
        this.userId = userId;
        this.balanceCents = balanceCents;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getWalletId() { return walletId; }
    public void setWalletId(Long walletId) { this.walletId = walletId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getBalanceCents() { return balanceCents; }
    public void setBalanceCents(Long balanceCents) { this.balanceCents = balanceCents; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    /** Helper to get balance as dollars for display */
    public String getBalanceFormatted() {
        return String.format("$%,.2f", balanceCents / 100.0);
    }
}

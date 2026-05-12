package com.marketplace.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "deposit_ledger")
public class DepositLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "deposit_id")
    private Long depositId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Column(name = "method", length = 50)
    private String method = "MANUAL";

    @Column(name = "reference_code", length = 100)
    private String referenceCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public DepositLedger() {}

    public DepositLedger(Long userId, Long amountCents, String method, String referenceCode) {
        this.userId = userId;
        this.amountCents = amountCents;
        this.method = method;
        this.referenceCode = referenceCode;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getDepositId() { return depositId; }
    public void setDepositId(Long depositId) { this.depositId = depositId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getAmountCents() { return amountCents; }
    public void setAmountCents(Long amountCents) { this.amountCents = amountCents; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getAmountFormatted() {
        return String.format("$%,.2f", amountCents / 100.0);
    }
}

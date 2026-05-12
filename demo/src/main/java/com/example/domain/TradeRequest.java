package com.example.domain;

public record TradeRequest(
        String fromAccountId,
        String toAccountId,
        long amountInCents,
        String otpCode
) {
}

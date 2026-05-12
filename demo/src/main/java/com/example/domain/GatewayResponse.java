package com.example.domain;

public record GatewayResponse(
        boolean success,
        String message,
        String reference
) {
}

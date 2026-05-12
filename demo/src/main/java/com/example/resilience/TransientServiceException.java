package com.example.resilience;

public class TransientServiceException extends RuntimeException {
    public TransientServiceException(String message) {
        super(message);
    }
}

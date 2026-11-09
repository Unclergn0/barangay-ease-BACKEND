package com.muntinlupa.barangay_ease.ai;

/** Thrown when no AI provider could answer (not configured, rate-limited, or down). */
public class AiUnavailableException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final boolean notConfigured;

    public AiUnavailableException(String message, boolean notConfigured) {
        super(message);
        this.notConfigured = notConfigured;
    }

    public boolean isNotConfigured() {
        return notConfigured;
    }
}

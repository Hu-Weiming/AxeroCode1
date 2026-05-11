package com.axercode.provider.api;

/**
 * Friendly provider-layer exception that preserves the source adapter and operation name.
 */
public class ProviderException extends RuntimeException {

    private final String providerName;
    private final String operation;

    public ProviderException(String providerName, String operation, String message) {
        super(message);
        this.providerName = providerName;
        this.operation = operation;
    }

    public ProviderException(String providerName, String operation, String message, Throwable cause) {
        super(message, cause);
        this.providerName = providerName;
        this.operation = operation;
    }

    public String providerName() {
        return providerName;
    }

    public String operation() {
        return operation;
    }
}

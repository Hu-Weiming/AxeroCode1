package com.axercode.provider.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class ProviderExceptionTest {

    @Test
    void storesProviderMetadataAndCause() {
        RuntimeException cause = new RuntimeException("connection reset");

        ProviderException exception = new ProviderException(
                "ollama",
                "generate",
                "Failed to call Ollama /api/chat",
                cause
        );

        assertEquals("ollama", exception.providerName());
        assertEquals("generate", exception.operation());
        assertEquals("Failed to call Ollama /api/chat", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}

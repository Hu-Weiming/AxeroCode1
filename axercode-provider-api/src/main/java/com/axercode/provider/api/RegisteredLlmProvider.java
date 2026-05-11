package com.axercode.provider.api;

import java.util.Objects;

/**
 * One provider registration entry containing runtime behavior plus UI metadata.
 */
public record RegisteredLlmProvider(
        ProviderDescriptor descriptor,
        LlmProvider provider
) {

    public RegisteredLlmProvider {
        Objects.requireNonNull(descriptor, "descriptor must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
    }
}

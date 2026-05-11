package com.axercode.provider.api;

/**
 * UI- and routing-facing metadata about one configured provider entry.
 */
public record ProviderDescriptor(
        String id,
        String label,
        String defaultModel,
        boolean configured,
        boolean comingSoon,
        boolean supportsStreaming
) {

    public ProviderDescriptor {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        if (defaultModel == null || defaultModel.isBlank()) {
            throw new IllegalArgumentException("defaultModel must not be blank");
        }
    }
}

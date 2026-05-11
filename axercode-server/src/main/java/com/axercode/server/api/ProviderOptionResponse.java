package com.axercode.server.api;

import com.axercode.provider.api.ProviderDescriptor;

public record ProviderOptionResponse(
        String id,
        String label,
        String defaultModel,
        boolean configured,
        boolean comingSoon,
        boolean supportsStreaming
) {

    public static ProviderOptionResponse from(ProviderDescriptor descriptor) {
        return new ProviderOptionResponse(
                descriptor.id(),
                descriptor.label(),
                descriptor.defaultModel(),
                descriptor.configured(),
                descriptor.comingSoon(),
                descriptor.supportsStreaming()
        );
    }
}

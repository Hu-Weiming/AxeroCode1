package com.axercode.provider.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves configured providers by id and exposes their routing metadata.
 */
public final class LlmProviderRegistry {

    private final String defaultProviderId;
    private final Map<String, RegisteredLlmProvider> providers;

    public LlmProviderRegistry(String defaultProviderId, List<RegisteredLlmProvider> providers) {
        if (defaultProviderId == null || defaultProviderId.isBlank()) {
            throw new IllegalArgumentException("defaultProviderId must not be blank");
        }
        Objects.requireNonNull(providers, "providers must not be null");
        if (providers.isEmpty()) {
            throw new IllegalArgumentException("providers must not be empty");
        }
        LinkedHashMap<String, RegisteredLlmProvider> mapped = new LinkedHashMap<>();
        for (RegisteredLlmProvider provider : providers) {
            String providerId = provider.descriptor().id();
            if (mapped.putIfAbsent(providerId, provider) != null) {
                throw new IllegalArgumentException("Duplicate provider id: " + providerId);
            }
        }
        if (!mapped.containsKey(defaultProviderId)) {
            throw new IllegalArgumentException("Unknown default provider id: " + defaultProviderId);
        }
        this.defaultProviderId = defaultProviderId;
        this.providers = Map.copyOf(mapped);
    }

    public String defaultProviderId() {
        return defaultProviderId;
    }

    public List<ProviderDescriptor> descriptors() {
        return providers.values().stream().map(RegisteredLlmProvider::descriptor).toList();
    }

    public RegisteredLlmProvider requireProvider(String providerId) {
        String resolvedProviderId = resolveProviderId(providerId);
        RegisteredLlmProvider registeredProvider = providers.get(resolvedProviderId);
        if (registeredProvider == null) {
            throw new IllegalArgumentException("Unknown provider: " + resolvedProviderId);
        }
        return registeredProvider;
    }

    public String resolveProviderId(String providerId) {
        return providerId == null || providerId.isBlank() ? defaultProviderId : providerId;
    }

    public String resolveModel(String providerId, String modelOverride) {
        if (modelOverride != null && !modelOverride.isBlank()) {
            return modelOverride;
        }
        return requireProvider(providerId).descriptor().defaultModel();
    }

    public String composeModel(String providerId, String modelOverride) {
        String resolvedProviderId = resolveProviderId(providerId);
        return ProviderModelRef.compose(resolvedProviderId, resolveModel(resolvedProviderId, modelOverride));
    }
}

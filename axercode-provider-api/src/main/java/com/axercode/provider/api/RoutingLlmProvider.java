package com.axercode.provider.api;

import com.axercode.core.provider.ProviderRequest;
import com.axercode.core.provider.ProviderResponse;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Delegates each provider request to the selected underlying provider based on the composite model reference.
 */
public final class RoutingLlmProvider implements LlmProvider {

    private final LlmProviderRegistry providerRegistry;

    public RoutingLlmProvider(LlmProviderRegistry providerRegistry) {
        this.providerRegistry = Objects.requireNonNull(providerRegistry, "providerRegistry must not be null");
    }

    @Override
    public String providerName() {
        return "router";
    }

    @Override
    public ProviderResponse generate(ProviderRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        ProviderModelRef modelRef = ProviderModelRef.parse(request.model(), providerRegistry.defaultProviderId());
        return delegate(modelRef, request).generate(rewriteRequest(request, modelRef));
    }

    @Override
    public ProviderResponse streamGenerate(ProviderRequest request, Consumer<String> textDeltaConsumer) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(textDeltaConsumer, "textDeltaConsumer must not be null");
        ProviderModelRef modelRef = ProviderModelRef.parse(request.model(), providerRegistry.defaultProviderId());
        return delegate(modelRef, request).streamGenerate(rewriteRequest(request, modelRef), textDeltaConsumer);
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    private LlmProvider delegate(ProviderModelRef modelRef, ProviderRequest request) {
        try {
            return providerRegistry.requireProvider(modelRef.providerId()).provider();
        } catch (IllegalArgumentException exception) {
            throw new ProviderException(providerName(), request.stream() ? "streamGenerate" : "generate", exception.getMessage(), exception);
        }
    }

    private ProviderRequest rewriteRequest(ProviderRequest request, ProviderModelRef modelRef) {
        return ProviderRequest.create(
                modelRef.model(),
                request.messages(),
                request.availableTools(),
                request.stream(),
                request.recentToolRound()
        );
    }
}

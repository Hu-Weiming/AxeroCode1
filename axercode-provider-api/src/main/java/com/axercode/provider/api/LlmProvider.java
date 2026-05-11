package com.axercode.provider.api;

import com.axercode.core.provider.ProviderRequest;
import com.axercode.core.provider.ProviderResponse;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Unified entry point for concrete LLM adapters such as Ollama or OpenAI-compatible providers.
 */
public interface LlmProvider {

    String providerName();

    ProviderResponse generate(ProviderRequest request);

    default ProviderResponse streamGenerate(ProviderRequest request, Consumer<String> textDeltaConsumer) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(textDeltaConsumer, "textDeltaConsumer must not be null");
        throw new ProviderException(
                providerName(),
                "streamGenerate",
                "Streaming is not supported by provider: " + providerName()
        );
    }

    default boolean supportsStreaming() {
        return false;
    }
}

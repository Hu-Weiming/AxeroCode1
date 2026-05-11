package com.axercode.provider.openai;

import com.axercode.core.provider.ProviderRequest;
import com.axercode.core.provider.ProviderResponse;
import com.axercode.provider.api.LlmProvider;
import com.axercode.provider.api.ProviderException;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Reserved OpenAI-compatible provider slot. The UI can already route to this provider id while the full implementation
 * remains in progress.
 */
public final class OpenAiCompatiblePlaceholderProvider implements LlmProvider {

    private static final String MESSAGE = "当前功能开发难度较大，目前正在加急修复中。";

    @Override
    public String providerName() {
        return "openai";
    }

    @Override
    public ProviderResponse generate(ProviderRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        throw new ProviderException(providerName(), "generate", MESSAGE);
    }

    @Override
    public ProviderResponse streamGenerate(ProviderRequest request, Consumer<String> textDeltaConsumer) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(textDeltaConsumer, "textDeltaConsumer must not be null");
        throw new ProviderException(providerName(), "streamGenerate", MESSAGE);
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }
}

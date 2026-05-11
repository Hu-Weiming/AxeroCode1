package com.axercode.cli.config;

import com.axercode.provider.anthropic.AnthropicMessagesProvider;
import com.axercode.provider.api.LlmProviderRegistry;
import com.axercode.provider.api.ProviderDescriptor;
import com.axercode.provider.api.ProviderIds;
import com.axercode.provider.api.LlmProvider;
import com.axercode.provider.api.RegisteredLlmProvider;
import com.axercode.provider.api.RoutingLlmProvider;
import com.axercode.provider.ollama.OllamaChatProvider;
import com.axercode.provider.ollama.OllamaRuntimeHints;
import com.axercode.provider.openai.OpenAiCompatiblePlaceholderProvider;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ImportRuntimeHints(OllamaRuntimeHints.class)
public class ProviderConfiguration {

    @Bean
    public LlmProviderRegistry llmProviderRegistry(AxerCodeProviderProperties properties) {
        List<RegisteredLlmProvider> providers = new ArrayList<>();

        if (properties.getOllama().isEnabled()) {
            providers.add(new RegisteredLlmProvider(
                    new ProviderDescriptor(
                            ProviderIds.OLLAMA,
                            "Ollama",
                            properties.resolveDefaultModel(ProviderIds.OLLAMA),
                            true,
                            false,
                            true
                    ),
                    new OllamaChatProvider(properties.getOllama().getBaseUrl())
            ));
        }

        if (properties.getAnthropic().isEnabled()) {
            providers.add(new RegisteredLlmProvider(
                    new ProviderDescriptor(
                            ProviderIds.ANTHROPIC,
                            "Anthropic",
                            properties.resolveDefaultModel(ProviderIds.ANTHROPIC),
                            properties.getAnthropic().getApiKey() != null && !properties.getAnthropic().getApiKey().isBlank(),
                            false,
                            true
                    ),
                    new AnthropicMessagesProvider(
                            properties.getAnthropic().getBaseUrl(),
                            properties.getAnthropic().getApiKey(),
                            properties.getAnthropic().getVersion(),
                            properties.getAnthropic().getMaxTokens()
                    )
            ));
        }

        if (properties.getOpenai().isEnabled()) {
            providers.add(new RegisteredLlmProvider(
                    new ProviderDescriptor(
                            ProviderIds.OPENAI,
                            "OpenAI-compatible",
                            properties.resolveDefaultModel(ProviderIds.OPENAI),
                            properties.getOpenai().getApiKey() != null && !properties.getOpenai().getApiKey().isBlank(),
                            true,
                            true
                    ),
                    new OpenAiCompatiblePlaceholderProvider()
            ));
        }

        return new LlmProviderRegistry(properties.getDefaultProvider(), providers);
    }

    @Bean
    public LlmProvider llmProvider(LlmProviderRegistry providerRegistry) {
        return new RoutingLlmProvider(providerRegistry);
    }
}

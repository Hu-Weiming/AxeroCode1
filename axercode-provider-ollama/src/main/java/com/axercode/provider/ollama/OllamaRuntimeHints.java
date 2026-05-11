package com.axercode.provider.ollama;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Native-image reflection hints for Jackson binding of Ollama request/response records.
 */
public final class OllamaRuntimeHints implements RuntimeHintsRegistrar {

    private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        bindingRegistrar.registerReflectionHints(
                hints.reflection(),
                OllamaChatRequest.class,
                OllamaChatRequest.OllamaChatMessage.class,
                OllamaChatRequest.OllamaToolDefinition.class,
                OllamaChatRequest.OllamaFunctionDefinition.class,
                OllamaChatResponse.class,
                OllamaChatResponse.OllamaChatMessage.class,
                OllamaChatResponse.OllamaToolCall.class,
                OllamaChatResponse.OllamaFunction.class
        );
    }
}

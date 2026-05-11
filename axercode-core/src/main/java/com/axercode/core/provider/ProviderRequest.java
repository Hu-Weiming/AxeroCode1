package com.axercode.core.provider;

import com.axercode.core.session.ConversationMessage;
import com.axercode.core.tool.ToolDefinition;
import java.util.List;
import java.util.Objects;

/**
 * Immutable prompt bundle handed from the agent layer to a concrete LLM provider adapter.
 */
public record ProviderRequest(
        String model,
        List<ConversationMessage> messages,
        List<ToolDefinition> availableTools,
        boolean stream,
        ProviderToolRound recentToolRound
) {

    public ProviderRequest {
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("model must not be blank");
        }
        Objects.requireNonNull(messages, "messages must not be null");
        Objects.requireNonNull(availableTools, "availableTools must not be null");
        if (messages.isEmpty()) {
            throw new IllegalArgumentException("messages must not be empty");
        }
        messages = List.copyOf(messages);
        availableTools = List.copyOf(availableTools);
    }

    public static ProviderRequest create(
            String model,
            List<ConversationMessage> messages,
            List<ToolDefinition> availableTools,
            boolean stream
    ) {
        return new ProviderRequest(model, messages, availableTools, stream, null);
    }

    public static ProviderRequest create(
            String model,
            List<ConversationMessage> messages,
            List<ToolDefinition> availableTools,
            boolean stream,
            ProviderToolRound recentToolRound
    ) {
        return new ProviderRequest(model, messages, availableTools, stream, recentToolRound);
    }
}

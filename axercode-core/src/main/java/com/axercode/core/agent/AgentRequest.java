package com.axercode.core.agent;

import com.axercode.core.session.ConversationMessage;
import java.util.List;
import java.util.Objects;

/**
 * Minimal immutable request sent from the shell or server into the agent layer.
 */
public record AgentRequest(List<ConversationMessage> messages) {

    public AgentRequest {
        Objects.requireNonNull(messages, "messages must not be null");
        if (messages.isEmpty()) {
            throw new IllegalArgumentException("messages must not be empty");
        }
        messages = List.copyOf(messages);
    }

    public static AgentRequest create(List<ConversationMessage> messages) {
        return new AgentRequest(messages);
    }
}

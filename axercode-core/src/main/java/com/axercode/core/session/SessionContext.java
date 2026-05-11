package com.axercode.core.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable snapshot of a conversation session and its accumulated messages.
 */
public record SessionContext(SessionId sessionId, List<ConversationMessage> messages) {

    public SessionContext {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(messages, "messages must not be null");
        messages = List.copyOf(messages);
    }

    public static SessionContext start() {
        return new SessionContext(SessionId.create(), List.of());
    }

    public SessionContext append(ConversationMessage message) {
        Objects.requireNonNull(message, "message must not be null");
        List<ConversationMessage> updatedMessages = new ArrayList<>(messages);
        updatedMessages.add(message);
        return new SessionContext(sessionId, updatedMessages);
    }
}

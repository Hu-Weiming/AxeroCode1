package com.axercode.core.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds a bounded provider-facing session snapshot while preserving all system messages.
 */
public final class SessionContextWindow {

    private final int maxRecentMessages;

    public SessionContextWindow(int maxRecentMessages) {
        if (maxRecentMessages < 1) {
            throw new IllegalArgumentException("maxRecentMessages must be at least 1");
        }
        this.maxRecentMessages = maxRecentMessages;
    }

    public SessionContext trim(SessionContext sessionContext) {
        Objects.requireNonNull(sessionContext, "sessionContext must not be null");

        long nonSystemCount = sessionContext.messages().stream()
                .filter(message -> message.role() != MessageRole.SYSTEM)
                .count();
        if (nonSystemCount <= maxRecentMessages) {
            return sessionContext;
        }

        long skipNonSystem = nonSystemCount - maxRecentMessages;
        long skipped = 0;
        List<ConversationMessage> trimmed = new ArrayList<>();
        for (ConversationMessage message : sessionContext.messages()) {
            if (message.role() == MessageRole.SYSTEM) {
                trimmed.add(message);
                continue;
            }
            if (skipped < skipNonSystem) {
                skipped++;
                continue;
            }
            trimmed.add(message);
        }
        return new SessionContext(sessionContext.sessionId(), trimmed);
    }

    public int maxRecentMessages() {
        return maxRecentMessages;
    }
}

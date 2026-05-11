package com.axercode.core.session;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Clones a session into a new branch session with fresh ids so it can be persisted independently.
 */
public final class SessionContextBrancher {

    private SessionContextBrancher() {
    }

    public static SessionContext branch(SessionContext source) {
        Objects.requireNonNull(source, "source must not be null");

        List<ConversationMessage> clonedMessages = source.messages().stream()
                .map(message -> new ConversationMessage(
                        UUID.randomUUID(),
                        message.role(),
                        message.content(),
                        message.createdAt()
                ))
                .toList();
        return new SessionContext(SessionId.create(), clonedMessages);
    }
}

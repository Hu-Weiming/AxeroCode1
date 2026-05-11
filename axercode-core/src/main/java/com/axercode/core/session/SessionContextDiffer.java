package com.axercode.core.session;

import java.util.List;
import java.util.Objects;

/**
 * Compares two sessions by their shared prefix and differing tails.
 */
public final class SessionContextDiffer {

    private SessionContextDiffer() {
    }

    public static SessionDiff diff(SessionContext current, SessionContext reference) {
        Objects.requireNonNull(current, "current must not be null");
        Objects.requireNonNull(reference, "reference must not be null");

        List<ConversationMessage> currentMessages = current.messages();
        List<ConversationMessage> referenceMessages = reference.messages();

        int prefix = 0;
        int maxPrefix = Math.min(currentMessages.size(), referenceMessages.size());
        while (prefix < maxPrefix && sameMessage(currentMessages.get(prefix), referenceMessages.get(prefix))) {
            prefix++;
        }

        return new SessionDiff(
                prefix,
                currentMessages.subList(prefix, currentMessages.size()),
                referenceMessages.subList(prefix, referenceMessages.size())
        );
    }

    private static boolean sameMessage(ConversationMessage left, ConversationMessage right) {
        return left.role() == right.role() && left.content().equals(right.content());
    }
}

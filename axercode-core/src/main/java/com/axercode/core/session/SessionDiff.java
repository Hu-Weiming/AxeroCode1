package com.axercode.core.session;

import java.util.List;
import java.util.Objects;

/**
 * Message-level difference between a current session and a reference session.
 */
public record SessionDiff(
        int commonPrefixCount,
        List<ConversationMessage> currentOnlyMessages,
        List<ConversationMessage> referenceOnlyMessages
) {

    public SessionDiff {
        if (commonPrefixCount < 0) {
            throw new IllegalArgumentException("commonPrefixCount must not be negative");
        }
        Objects.requireNonNull(currentOnlyMessages, "currentOnlyMessages must not be null");
        Objects.requireNonNull(referenceOnlyMessages, "referenceOnlyMessages must not be null");
        currentOnlyMessages = List.copyOf(currentOnlyMessages);
        referenceOnlyMessages = List.copyOf(referenceOnlyMessages);
    }

    public boolean identical() {
        return currentOnlyMessages.isEmpty() && referenceOnlyMessages.isEmpty();
    }
}

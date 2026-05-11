package com.axercode.agent;

import com.axercode.core.session.ConversationMessage;
import java.util.List;

/**
 * Supplies user interjections that should be appended before the next provider round.
 */
@FunctionalInterface
public interface TurnInterruptionSource {

    TurnInterruptionSource NONE = List::of;

    List<ConversationMessage> drainPendingMessages();

    static TurnInterruptionSource none() {
        return NONE;
    }
}

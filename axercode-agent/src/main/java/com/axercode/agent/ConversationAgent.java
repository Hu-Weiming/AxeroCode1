package com.axercode.agent;

import com.axercode.core.session.SessionContext;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Minimal agent boundary for continuing a conversation turn from an existing session snapshot.
 */
public interface ConversationAgent {

    AgentConversationTurn continueConversation(SessionContext sessionContext, String prompt, String model);

    default AgentConversationTurn continueConversation(
            SessionContext sessionContext,
            String prompt,
            String model,
            TurnInterruptionSource turnInterruptionSource
    ) {
        Objects.requireNonNull(turnInterruptionSource, "turnInterruptionSource must not be null");
        return continueConversation(sessionContext, prompt, model);
    }

    default AgentConversationTurn continueConversationStreaming(
            SessionContext sessionContext,
            String prompt,
            String model,
            Consumer<String> textDeltaConsumer
    ) {
        Objects.requireNonNull(textDeltaConsumer, "textDeltaConsumer must not be null");
        return continueConversation(sessionContext, prompt, model);
    }

    default AgentConversationTurn continueConversationStreaming(
            SessionContext sessionContext,
            String prompt,
            String model,
            Consumer<String> textDeltaConsumer,
            TurnInterruptionSource turnInterruptionSource
    ) {
        Objects.requireNonNull(textDeltaConsumer, "textDeltaConsumer must not be null");
        Objects.requireNonNull(turnInterruptionSource, "turnInterruptionSource must not be null");
        return continueConversationStreaming(sessionContext, prompt, model, textDeltaConsumer);
    }
}

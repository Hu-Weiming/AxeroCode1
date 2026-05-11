package com.axercode.agent;

import com.axercode.core.session.SessionContext;
import com.axercode.core.tool.ToolExecutionResult;
import java.util.List;
import java.util.Objects;

/**
 * Result of one agent conversation turn, including the updated session, final reply, and any executed tool results.
 */
public record AgentConversationTurn(
        SessionContext sessionContext,
        String reply,
        List<ToolExecutionResult> toolResults
) {

    public AgentConversationTurn {
        Objects.requireNonNull(sessionContext, "sessionContext must not be null");
        if (reply == null || reply.isBlank()) {
            throw new IllegalArgumentException("reply must not be blank");
        }
        Objects.requireNonNull(toolResults, "toolResults must not be null");
        toolResults = List.copyOf(toolResults);
    }
}

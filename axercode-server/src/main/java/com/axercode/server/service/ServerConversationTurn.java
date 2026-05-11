package com.axercode.server.service;

import com.axercode.core.session.SessionContext;
import com.axercode.core.tool.ToolExecutionResult;
import java.util.List;
import java.util.Objects;

/**
 * Server-facing conversation result with the persisted session snapshot and final reply.
 */
public record ServerConversationTurn(
        SessionContext sessionContext,
        String reply,
        List<ToolExecutionResult> toolResults,
        String provider,
        String model
) {

    public ServerConversationTurn {
        Objects.requireNonNull(sessionContext, "sessionContext must not be null");
        if (reply == null || reply.isBlank()) {
            throw new IllegalArgumentException("reply must not be blank");
        }
        Objects.requireNonNull(toolResults, "toolResults must not be null");
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("provider must not be blank");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("model must not be blank");
        }
        toolResults = List.copyOf(toolResults);
    }
}

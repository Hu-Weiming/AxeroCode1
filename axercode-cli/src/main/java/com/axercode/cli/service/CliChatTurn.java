package com.axercode.cli.service;

import com.axercode.core.session.SessionContext;
import com.axercode.core.tool.ToolExecutionResult;
import java.util.List;
import java.util.Objects;

/**
 * Result of a single CLI chat turn, carrying both printable reply text and updated session state.
 */
public record CliChatTurn(SessionContext sessionContext, String reply, List<ToolExecutionResult> toolResults) {

    public CliChatTurn {
        Objects.requireNonNull(sessionContext, "sessionContext must not be null");
        if (reply == null || reply.isBlank()) {
            throw new IllegalArgumentException("reply must not be blank");
        }
        Objects.requireNonNull(toolResults, "toolResults must not be null");
        toolResults = List.copyOf(toolResults);
    }
}

package com.axercode.core.provider;

import com.axercode.core.tool.ToolCall;
import com.axercode.core.tool.ToolExecutionResult;
import java.util.List;
import java.util.Objects;

/**
 * Structured record of the most recent tool-use round so providers can map tool calls and results back into
 * protocol-specific follow-up payloads.
 */
public record ProviderToolRound(
        List<ToolCall> toolCalls,
        List<ToolExecutionResult> toolResults
) {

    public ProviderToolRound {
        Objects.requireNonNull(toolCalls, "toolCalls must not be null");
        Objects.requireNonNull(toolResults, "toolResults must not be null");
        toolCalls = List.copyOf(toolCalls);
        toolResults = List.copyOf(toolResults);
        if (toolCalls.isEmpty()) {
            throw new IllegalArgumentException("toolCalls must not be empty");
        }
        if (toolResults.isEmpty()) {
            throw new IllegalArgumentException("toolResults must not be empty");
        }
    }
}

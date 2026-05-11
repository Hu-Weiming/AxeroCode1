package com.axercode.core.tool;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable observation emitted after a tool call finishes.
 */
public record ToolExecutionResult(
        ToolCall toolCall,
        ToolExecutionStatus status,
        String output,
        Instant createdAt
) {

    public ToolExecutionResult {
        Objects.requireNonNull(toolCall, "toolCall must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (output == null || output.isBlank()) {
            throw new IllegalArgumentException("output must not be blank");
        }
    }

    public static ToolExecutionResult success(ToolCall toolCall, String output) {
        return new ToolExecutionResult(toolCall, ToolExecutionStatus.SUCCESS, output, Instant.now());
    }

    public static ToolExecutionResult failure(ToolCall toolCall, String output) {
        return new ToolExecutionResult(toolCall, ToolExecutionStatus.FAILURE, output, Instant.now());
    }
}

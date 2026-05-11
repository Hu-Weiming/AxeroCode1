package com.axercode.core.provider;

import com.axercode.core.tool.ToolCall;
import java.util.List;
import java.util.Objects;

/**
 * Normalized provider output containing either assistant text, tool calls, or both.
 */
public record ProviderResponse(
        String content,
        List<ToolCall> toolCalls,
        ProviderStopReason stopReason
) {

    public ProviderResponse {
        Objects.requireNonNull(toolCalls, "toolCalls must not be null");
        Objects.requireNonNull(stopReason, "stopReason must not be null");
        content = content == null ? "" : content;
        toolCalls = List.copyOf(toolCalls);
        if (content.isBlank() && toolCalls.isEmpty()) {
            throw new IllegalArgumentException("response must contain content or toolCalls");
        }
    }

    public static ProviderResponse complete(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        return new ProviderResponse(content, List.of(), ProviderStopReason.COMPLETE);
    }

    public static ProviderResponse toolCalls(List<ToolCall> toolCalls) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            throw new IllegalArgumentException("toolCalls must not be empty");
        }
        return new ProviderResponse("", toolCalls, ProviderStopReason.TOOL_CALLS);
    }
}

package com.axercode.tools.execution;

import com.axercode.core.tool.ToolCall;
import com.axercode.core.tool.ToolExecutionResult;
import com.axercode.tools.registry.ToolRegistry;

/**
 * Dispatches tool calls to registered tool implementations.
 */
public class ToolExecutor {

    private final ToolRegistry toolRegistry;

    public ToolExecutor(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    public ToolExecutionResult execute(ToolCall toolCall) {
        return toolRegistry.find(toolCall.name())
                .map(tool -> tool.execute(toolCall))
                .orElseGet(() -> ToolExecutionResult.failure(toolCall, "Unknown tool: " + toolCall.name()));
    }
}

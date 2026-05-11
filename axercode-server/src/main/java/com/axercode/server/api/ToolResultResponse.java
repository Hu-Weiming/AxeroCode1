package com.axercode.server.api;

import com.axercode.core.tool.ToolExecutionResult;

public record ToolResultResponse(
        String toolName,
        String status,
        String output
) {

    public static ToolResultResponse from(ToolExecutionResult result) {
        return new ToolResultResponse(
                result.toolCall().name(),
                result.status().name(),
                result.output()
        );
    }
}

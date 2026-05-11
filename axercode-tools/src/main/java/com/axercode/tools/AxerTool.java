package com.axercode.tools;

import com.axercode.core.tool.ToolDefinition;
import com.axercode.core.tool.ToolCall;
import com.axercode.core.tool.ToolExecutionResult;

/**
 * One executable local tool exposed to the future agent layer.
 */
public interface AxerTool {

    ToolDefinition definition();

    ToolExecutionResult execute(ToolCall toolCall);
}

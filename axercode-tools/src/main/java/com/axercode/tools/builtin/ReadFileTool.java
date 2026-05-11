package com.axercode.tools.builtin;

import com.axercode.core.tool.ToolCall;
import com.axercode.core.tool.ToolDefinition;
import com.axercode.core.tool.ToolExecutionResult;
import com.axercode.tools.AxerTool;
import com.axercode.tools.execution.ToolArguments;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads the full content of a local file.
 */
public class ReadFileTool implements AxerTool {

    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "read_file",
            "Read the full text content of a local file.",
            """
            {"type":"object","required":["path"],"properties":{"path":{"type":"string"}}}
            """
    );

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public ToolExecutionResult execute(ToolCall toolCall) {
        try {
            ToolArguments arguments = new ToolArguments(toolCall.argumentsJson());
            Path path = Path.of(arguments.requiredString("path"));
            return ToolExecutionResult.success(toolCall, Files.readString(path));
        } catch (Exception exception) {
            return ToolExecutionResult.failure(toolCall, exception.getMessage());
        }
    }
}

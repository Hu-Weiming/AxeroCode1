package com.axercode.tools.builtin;

import com.axercode.core.tool.ToolCall;
import com.axercode.core.tool.ToolDefinition;
import com.axercode.core.tool.ToolExecutionResult;
import com.axercode.tools.AxerTool;
import com.axercode.tools.execution.ToolArguments;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Creates a local directory in a structured, verified way.
 */
public class CreateDirectoryTool implements AxerTool {

    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "create_directory",
            "Create a local directory path. Prefer this over shell commands for directory creation tasks.",
            """
            {"type":"object","required":["path"],"properties":{"path":{"type":"string"},"createParents":{"type":"boolean"}}}
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
            Path target = Path.of(arguments.requiredString("path"));
            boolean createParents = arguments.optionalBoolean("createParents", true);

            if (Files.isDirectory(target)) {
                return ToolExecutionResult.success(toolCall, "Directory already exists: " + target.toAbsolutePath());
            }

            if (createParents) {
                Files.createDirectories(target);
            } else {
                Files.createDirectory(target);
            }

            return ToolExecutionResult.success(toolCall, "Created directory: " + target.toAbsolutePath());
        } catch (Exception exception) {
            return ToolExecutionResult.failure(toolCall, describeException(exception));
        }
    }

    private String describeException(Exception exception) {
        if (exception instanceof IllegalArgumentException) {
            return exception.getMessage();
        }
        return exception.getClass().getSimpleName() + ": " + exception.getMessage();
    }
}

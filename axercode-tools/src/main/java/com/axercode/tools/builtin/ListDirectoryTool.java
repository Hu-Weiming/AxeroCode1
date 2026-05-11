package com.axercode.tools.builtin;

import com.axercode.core.tool.ToolCall;
import com.axercode.core.tool.ToolDefinition;
import com.axercode.core.tool.ToolExecutionResult;
import com.axercode.tools.AxerTool;
import com.axercode.tools.execution.ToolArguments;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Lists entries under a directory, optionally including nested entries.
 */
public class ListDirectoryTool implements AxerTool {

    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "list_directory",
            "List files and directories under a local path.",
            """
            {"type":"object","required":["path"],"properties":{"path":{"type":"string"},"recursive":{"type":"boolean"}}}
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
            Path root = Path.of(arguments.requiredString("path"));
            boolean recursive = arguments.optionalBoolean("recursive", false);

            try (Stream<Path> stream = recursive ? Files.walk(root) : Files.list(root)) {
                String output = stream
                        .filter(path -> !path.equals(root))
                        .sorted(Comparator.comparing(Path::toString))
                        .map(path -> formatEntry(root, path))
                        .reduce((left, right) -> left + System.lineSeparator() + right)
                        .orElse("[AxerCode] Directory is empty.");
                return ToolExecutionResult.success(toolCall, output);
            }
        } catch (Exception exception) {
            return ToolExecutionResult.failure(toolCall, exception.getMessage());
        }
    }

    private String formatEntry(Path root, Path path) {
        String relativePath = root.relativize(path).toString().replace('\\', '/');
        try {
            return Files.isDirectory(path) ? "[DIR] " + relativePath : "[FILE] " + relativePath;
        } catch (Exception exception) {
            return "[UNKNOWN] " + relativePath;
        }
    }
}

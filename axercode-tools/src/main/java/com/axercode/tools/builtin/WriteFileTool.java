package com.axercode.tools.builtin;

import com.axercode.core.tool.ToolCall;
import com.axercode.core.tool.ToolDefinition;
import com.axercode.core.tool.ToolExecutionResult;
import com.axercode.tools.AxerTool;
import com.axercode.tools.execution.ToolArguments;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Writes UTF-8 text files with explicit overwrite and append controls.
 */
public class WriteFileTool implements AxerTool {

    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "write_file",
            "Write or append UTF-8 text to a local file. Prefer this over shell commands for file creation and edits.",
            """
            {"type":"object","required":["path","content"],"properties":{"path":{"type":"string"},"content":{"type":"string"},"overwrite":{"type":"boolean"},"append":{"type":"boolean"},"createParents":{"type":"boolean"}}}
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
            String content = arguments.requiredString("content");
            boolean overwrite = arguments.optionalBoolean("overwrite", true);
            boolean append = arguments.optionalBoolean("append", false);
            boolean createParents = arguments.optionalBoolean("createParents", true);

            ensureParentDirectory(target, createParents);

            if (append) {
                Files.writeString(
                        target,
                        content,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );
                return ToolExecutionResult.success(toolCall, "Appended file: " + target.toAbsolutePath());
            }

            if (Files.exists(target) && !overwrite) {
                return ToolExecutionResult.failure(
                        toolCall,
                        "File already exists and overwrite=false: " + target.toAbsolutePath()
                );
            }

            if (overwrite) {
                Files.writeString(
                        target,
                        content,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE
                );
            } else {
                Files.writeString(
                        target,
                        content,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE
                );
            }

            return ToolExecutionResult.success(toolCall, "Wrote file: " + target.toAbsolutePath());
        } catch (Exception exception) {
            return ToolExecutionResult.failure(toolCall, describeException(exception));
        }
    }

    private void ensureParentDirectory(Path target, boolean createParents) throws Exception {
        Path parent = target.getParent();
        if (parent == null || Files.isDirectory(parent)) {
            return;
        }
        if (createParents) {
            Files.createDirectories(parent);
            return;
        }
        throw new NoSuchFileException(parent.toAbsolutePath().toString());
    }

    private String describeException(Exception exception) {
        if (exception instanceof IllegalArgumentException) {
            return exception.getMessage();
        }
        return exception.getClass().getSimpleName() + ": " + exception.getMessage();
    }
}

package com.axercode.tools.builtin;

import com.axercode.core.tool.ToolCall;
import com.axercode.core.tool.ToolDefinition;
import com.axercode.core.tool.ToolExecutionResult;
import com.axercode.tools.AxerTool;
import com.axercode.tools.execution.ToolArguments;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Executes a local PowerShell command with a bounded timeout.
 */
public class RunShellTool implements AxerTool {

    private static final ToolDefinition DEFINITION = new ToolDefinition(
            "run_shell",
            "Run a local PowerShell command and capture its output.",
            """
            {"type":"object","required":["command"],"properties":{"command":{"type":"string"},"timeoutSeconds":{"type":"integer"}}}
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
            String command = arguments.requiredString("command");
            int timeoutSeconds = Math.max(1, Math.min(arguments.optionalInt("timeoutSeconds", 10), 30));

            Process process = new ProcessBuilder("powershell", "-NoProfile", "-Command", command)
                    .redirectErrorStream(true)
                    .start();

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return ToolExecutionResult.failure(toolCall, "Command timed out after " + timeoutSeconds + " seconds");
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0) {
                return ToolExecutionResult.failure(toolCall, output.isBlank() ? "Shell command failed" : output);
            }
            return ToolExecutionResult.success(toolCall, output.isBlank() ? "[AxerCode] Command completed with no output." : output);
        } catch (Exception exception) {
            return ToolExecutionResult.failure(toolCall, exception.getMessage());
        }
    }
}

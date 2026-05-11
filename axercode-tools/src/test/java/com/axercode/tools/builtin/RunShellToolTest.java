package com.axercode.tools.builtin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axercode.core.tool.ToolCall;
import com.axercode.core.tool.ToolExecutionResult;
import com.axercode.core.tool.ToolExecutionStatus;
import org.junit.jupiter.api.Test;

class RunShellToolTest {

    @Test
    void executeRunsShellCommandAndCapturesOutput() {
        RunShellTool tool = new RunShellTool();

        ToolExecutionResult result = tool.execute(ToolCall.create("run_shell", """
                {"command":"Write-Output hello-from-shell"}
                """));

        assertEquals(ToolExecutionStatus.SUCCESS, result.status());
        assertEquals("hello-from-shell", result.output());
    }

    @Test
    void executeReturnsFailureWhenCommandTimesOut() {
        RunShellTool tool = new RunShellTool();

        ToolExecutionResult result = tool.execute(ToolCall.create("run_shell", """
                {"command":"Start-Sleep -Seconds 2; Write-Output done","timeoutSeconds":1}
                """));

        assertEquals(ToolExecutionStatus.FAILURE, result.status());
        assertTrue(result.output().contains("Command timed out after 1 seconds"));
    }
}

package com.axercode.core.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ToolExecutionResultTest {

    @Test
    void successFactoryCapturesToolMetadata() {
        ToolCall call = ToolCall.create("read_file", "{\"path\":\"README.md\"}");

        ToolExecutionResult result = ToolExecutionResult.success(call, "file contents");

        assertEquals(call, result.toolCall());
        assertEquals(ToolExecutionStatus.SUCCESS, result.status());
        assertEquals("file contents", result.output());
        assertNotNull(result.createdAt());
    }

    @Test
    void failureFactoryCreatesFailureStatus() {
        ToolCall call = ToolCall.create("run_shell", "{\"command\":\"git status\"}");

        ToolExecutionResult result = ToolExecutionResult.failure(call, "command failed");

        assertEquals(ToolExecutionStatus.FAILURE, result.status());
        assertEquals("command failed", result.output());
    }

    @Test
    void constructorRejectsBlankOutput() {
        ToolCall call = ToolCall.create("ls", "{}");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ToolExecutionResult.success(call, " ")
        );

        assertEquals("output must not be blank", exception.getMessage());
    }
}

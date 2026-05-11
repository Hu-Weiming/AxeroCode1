package com.axercode.tools.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.axercode.core.tool.ToolCall;
import com.axercode.core.tool.ToolDefinition;
import com.axercode.core.tool.ToolExecutionResult;
import com.axercode.core.tool.ToolExecutionStatus;
import com.axercode.tools.AxerTool;
import com.axercode.tools.registry.ToolRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;

class ToolExecutorTest {

    @Test
    void executeDispatchesToRegisteredTool() {
        ToolRegistry registry = new ToolRegistry(List.of(new StubTool("read_file")));
        ToolExecutor executor = new ToolExecutor(registry);

        ToolExecutionResult result = executor.execute(ToolCall.create("read_file", "{\"path\":\"demo.txt\"}"));

        assertEquals(ToolExecutionStatus.SUCCESS, result.status());
        assertEquals("handled: read_file", result.output());
    }

    @Test
    void executeReturnsFailureForUnknownTool() {
        ToolRegistry registry = new ToolRegistry(List.of(new StubTool("read_file")));
        ToolExecutor executor = new ToolExecutor(registry);

        ToolExecutionResult result = executor.execute(ToolCall.create("missing_tool", "{}"));

        assertEquals(ToolExecutionStatus.FAILURE, result.status());
        assertEquals("Unknown tool: missing_tool", result.output());
    }

    private record StubTool(String name) implements AxerTool {

        @Override
        public ToolDefinition definition() {
            return new ToolDefinition(name, "stub", "{}");
        }

        @Override
        public ToolExecutionResult execute(ToolCall toolCall) {
            return ToolExecutionResult.success(toolCall, "handled: " + toolCall.name());
        }
    }
}

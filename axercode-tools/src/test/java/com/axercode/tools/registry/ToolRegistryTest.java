package com.axercode.tools.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.axercode.core.tool.ToolDefinition;
import com.axercode.core.tool.ToolCall;
import com.axercode.core.tool.ToolExecutionResult;
import com.axercode.tools.AxerTool;
import java.util.List;
import org.junit.jupiter.api.Test;

class ToolRegistryTest {

    @Test
    void registerAllowsLookupAndListsToolNames() {
        ToolRegistry registry = new ToolRegistry(List.of(new StubTool("read_file"), new StubTool("list_directory")));

        assertEquals("read_file", registry.find("read_file").orElseThrow().definition().name());
        assertEquals(List.of("list_directory", "read_file"), registry.availableToolNames());
        assertEquals(List.of("list_directory", "read_file"), registry.availableTools().stream().map(ToolDefinition::name).toList());
    }

    @Test
    void registerRejectsDuplicateToolNames() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new ToolRegistry(List.of(new StubTool("read_file"), new StubTool("read_file"))));

        assertEquals("Duplicate tool name: read_file", exception.getMessage());
    }

    private record StubTool(String name) implements AxerTool {

        @Override
        public ToolDefinition definition() {
            return new ToolDefinition(name, "stub", "{}");
        }

        @Override
        public ToolExecutionResult execute(ToolCall toolCall) {
            return ToolExecutionResult.success(toolCall, "ok");
        }
    }
}

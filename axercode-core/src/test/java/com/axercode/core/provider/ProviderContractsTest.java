package com.axercode.core.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.axercode.core.session.ConversationMessage;
import com.axercode.core.tool.ToolDefinition;
import com.axercode.core.tool.ToolCall;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProviderContractsTest {

    @Test
    void requestCopiesMessagesAndAvailableToolsDefensively() {
        List<ConversationMessage> messages = new ArrayList<>();
        messages.add(ConversationMessage.user("Summarize the project"));
        List<ToolDefinition> tools = new ArrayList<>();
        tools.add(new ToolDefinition("read_file", "Read file content", "{\"type\":\"object\"}"));

        ProviderRequest request = ProviderRequest.create("qwen2.5:7b", messages, tools, true);
        messages.clear();
        tools.clear();

        assertEquals(1, request.messages().size());
        assertEquals(1, request.availableTools().size());
        assertEquals("read_file", request.availableTools().getFirst().name());
        assertEquals("qwen2.5:7b", request.model());
        assertEquals(null, request.recentToolRound());
    }

    @Test
    void requestRejectsBlankModel() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ProviderRequest.create(" ", List.of(ConversationMessage.user("hi")), List.of(), true)
        );

        assertEquals("model must not be blank", exception.getMessage());
    }

    @Test
    void completeResponseRejectsBlankContent() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ProviderResponse.complete(" ")
        );

        assertEquals("content must not be blank", exception.getMessage());
    }

    @Test
    void toolCallResponseRequiresAtLeastOneToolCall() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ProviderResponse.toolCalls(List.of())
        );

        assertEquals("toolCalls must not be empty", exception.getMessage());
    }

    @Test
    void toolCallResponseCopiesToolCallsDefensively() {
        List<ToolCall> toolCalls = new ArrayList<>();
        toolCalls.add(ToolCall.create("read_file", "{\"path\":\"README.md\"}"));

        ProviderResponse response = ProviderResponse.toolCalls(toolCalls);
        toolCalls.clear();

        assertEquals(1, response.toolCalls().size());
        assertEquals(ProviderStopReason.TOOL_CALLS, response.stopReason());
    }
}

package com.axercode.provider.ollama;

import com.axercode.core.provider.ProviderRequest;
import com.axercode.core.session.ConversationMessage;
import com.axercode.core.session.MessageRole;
import com.axercode.core.tool.ToolDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

record OllamaChatRequest(
        String model,
        List<OllamaChatMessage> messages,
        List<OllamaToolDefinition> tools,
        boolean stream
) {

    static OllamaChatRequest from(ProviderRequest request, ObjectMapper objectMapper) {
        List<OllamaChatMessage> mappedMessages = request.messages().stream()
                .map(OllamaChatRequest::mapMessage)
                .toList();
        List<OllamaToolDefinition> mappedTools = request.availableTools().stream()
                .map(toolDefinition -> mapTool(toolDefinition, objectMapper))
                .toList();
        return new OllamaChatRequest(request.model(), mappedMessages, mappedTools, request.stream());
    }

    private static OllamaChatMessage mapMessage(ConversationMessage message) {
        return new OllamaChatMessage(mapRole(message.role()), message.content());
    }

    private static String mapRole(MessageRole role) {
        return switch (role) {
            case SYSTEM -> "system";
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case TOOL -> "tool";
        };
    }

    private static OllamaToolDefinition mapTool(ToolDefinition toolDefinition, ObjectMapper objectMapper) {
        try {
            JsonNode parameters = objectMapper.readTree(toolDefinition.parametersJsonSchema());
            return new OllamaToolDefinition(
                    "function",
                    new OllamaFunctionDefinition(toolDefinition.name(), toolDefinition.description(), parameters)
            );
        } catch (Exception exception) {
            throw new IllegalArgumentException("Failed to parse tool schema for " + toolDefinition.name(), exception);
        }
    }

    record OllamaChatMessage(String role, String content) {
    }

    record OllamaToolDefinition(String type, OllamaFunctionDefinition function) {
    }

    record OllamaFunctionDefinition(String name, String description, JsonNode parameters) {
    }
}

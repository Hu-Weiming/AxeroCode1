package com.axercode.tools.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Small JSON helper for extracting typed tool arguments from a tool call payload.
 */
public class ToolArguments {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JsonNode root;

    public ToolArguments(String argumentsJson) {
        try {
            this.root = OBJECT_MAPPER.readTree(argumentsJson == null || argumentsJson.isBlank() ? "{}" : argumentsJson);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid JSON arguments", exception);
        }
    }

    public String requiredString(String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull() || node.asText().isBlank()) {
            throw new IllegalArgumentException("Missing required string argument: " + fieldName);
        }
        return node.asText();
    }

    public boolean optionalBoolean(String fieldName, boolean defaultValue) {
        JsonNode node = root.get(fieldName);
        return node == null || node.isNull() ? defaultValue : node.asBoolean(defaultValue);
    }

    public int optionalInt(String fieldName, int defaultValue) {
        JsonNode node = root.get(fieldName);
        return node == null || node.isNull() ? defaultValue : node.asInt(defaultValue);
    }
}

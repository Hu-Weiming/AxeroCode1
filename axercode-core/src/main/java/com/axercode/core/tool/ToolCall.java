package com.axercode.core.tool;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable request from the agent to invoke a named tool with JSON arguments.
 */
public record ToolCall(String id, String name, String argumentsJson) {

    public ToolCall {
        Objects.requireNonNull(id, "id must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (argumentsJson == null || argumentsJson.isBlank()) {
            argumentsJson = "{}";
        }
    }

    public static ToolCall create(String name, String argumentsJson) {
        return new ToolCall(UUID.randomUUID().toString(), name, argumentsJson);
    }

    public static ToolCall create(String id, String name, String argumentsJson) {
        return new ToolCall(id, name, argumentsJson);
    }
}

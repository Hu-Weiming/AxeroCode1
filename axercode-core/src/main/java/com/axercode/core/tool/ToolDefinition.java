package com.axercode.core.tool;

import java.util.Objects;

/**
 * Shared metadata describing a tool name, what it does, and the JSON input schema it expects.
 */
public record ToolDefinition(String name, String description, String parametersJsonSchema) {

    public ToolDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        Objects.requireNonNull(parametersJsonSchema, "parametersJsonSchema must not be null");
    }
}

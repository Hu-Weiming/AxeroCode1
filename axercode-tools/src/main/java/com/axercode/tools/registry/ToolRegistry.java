package com.axercode.tools.registry;

import com.axercode.core.tool.ToolDefinition;
import com.axercode.tools.AxerTool;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Registry of all locally available tools keyed by tool name.
 */
public class ToolRegistry {

    private final Map<String, AxerTool> toolsByName;

    public ToolRegistry(Collection<? extends AxerTool> tools) {
        if (tools == null) {
            throw new IllegalArgumentException("tools must not be null");
        }

        LinkedHashMap<String, AxerTool> registeredTools = new LinkedHashMap<>();
        for (AxerTool tool : tools) {
            String toolName = tool.definition().name();
            if (registeredTools.containsKey(toolName)) {
                throw new IllegalArgumentException("Duplicate tool name: " + toolName);
            }
            registeredTools.put(toolName, tool);
        }
        this.toolsByName = Map.copyOf(registeredTools);
    }

    public Optional<AxerTool> find(String name) {
        return Optional.ofNullable(toolsByName.get(name));
    }

    public List<String> availableToolNames() {
        return List.copyOf(new TreeSet<>(toolsByName.keySet()));
    }

    public List<ToolDefinition> availableTools() {
        return toolsByName.values().stream()
                .map(AxerTool::definition)
                .sorted((left, right) -> left.name().compareTo(right.name()))
                .toList();
    }
}

package com.axercode.cli.service;

import com.axercode.core.tool.ToolExecutionResult;
import java.util.ArrayList;
import java.util.List;

/**
 * Formats executed tool results into compact CLI-friendly output blocks.
 */
public final class ToolFeedbackFormatter {

    public List<String> formatBlocks(List<ToolExecutionResult> toolResults) {
        List<String> blocks = new ArrayList<>();
        for (ToolExecutionResult toolResult : toolResults) {
            blocks.add("[tool] "
                    + toolResult.toolCall().name()
                    + " "
                    + toolResult.status().name()
                    + System.lineSeparator()
                    + toolResult.output());
        }
        return List.copyOf(blocks);
    }
}

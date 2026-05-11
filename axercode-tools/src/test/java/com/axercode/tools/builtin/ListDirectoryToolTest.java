package com.axercode.tools.builtin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axercode.core.tool.ToolCall;
import com.axercode.core.tool.ToolExecutionResult;
import com.axercode.core.tool.ToolExecutionStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ListDirectoryToolTest {

    @Test
    void executeListsImmediateDirectoryEntries(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("alpha.txt"), "a");
        Files.createDirectory(tempDir.resolve("nested"));
        ListDirectoryTool tool = new ListDirectoryTool();

        ToolExecutionResult result = tool.execute(ToolCall.create("list_directory", """
                {"path":"%s"}
                """.formatted(tempDir.toString().replace("\\", "\\\\"))));

        assertEquals(ToolExecutionStatus.SUCCESS, result.status());
        assertTrue(result.output().contains("[FILE] alpha.txt"));
        assertTrue(result.output().contains("[DIR] nested"));
    }

    @Test
    void executeListsNestedEntriesWhenRecursive(@TempDir Path tempDir) throws Exception {
        Path nested = Files.createDirectory(tempDir.resolve("nested"));
        Files.writeString(nested.resolve("beta.txt"), "b");
        ListDirectoryTool tool = new ListDirectoryTool();

        ToolExecutionResult result = tool.execute(ToolCall.create("list_directory", """
                {"path":"%s","recursive":true}
                """.formatted(tempDir.toString().replace("\\", "\\\\"))));

        assertEquals(ToolExecutionStatus.SUCCESS, result.status());
        assertTrue(result.output().contains("[DIR] nested"));
        assertTrue(result.output().contains("[FILE] nested/beta.txt"));
    }
}

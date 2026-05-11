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

class CreateDirectoryToolTest {

    @Test
    void executeCreatesNestedDirectoryByDefault(@TempDir Path tempDir) {
        Path target = tempDir.resolve("nested").resolve("src").resolve("main");
        CreateDirectoryTool tool = new CreateDirectoryTool();

        ToolExecutionResult result = tool.execute(ToolCall.create("create_directory", """
                {"path":"%s"}
                """.formatted(target.toString().replace("\\", "\\\\"))));

        assertEquals(ToolExecutionStatus.SUCCESS, result.status());
        assertTrue(Files.isDirectory(target));
        assertEquals("Created directory: " + target.toAbsolutePath(), result.output());
    }

    @Test
    void executeReturnsSuccessWhenDirectoryAlreadyExists(@TempDir Path tempDir) throws Exception {
        Path target = Files.createDirectories(tempDir.resolve("existing"));
        CreateDirectoryTool tool = new CreateDirectoryTool();

        ToolExecutionResult result = tool.execute(ToolCall.create("create_directory", """
                {"path":"%s"}
                """.formatted(target.toString().replace("\\", "\\\\"))));

        assertEquals(ToolExecutionStatus.SUCCESS, result.status());
        assertEquals("Directory already exists: " + target.toAbsolutePath(), result.output());
    }

    @Test
    void executeFailsWhenCreateParentsIsDisabledAndParentIsMissing(@TempDir Path tempDir) {
        Path target = tempDir.resolve("missing").resolve("child");
        CreateDirectoryTool tool = new CreateDirectoryTool();

        ToolExecutionResult result = tool.execute(ToolCall.create("create_directory", """
                {"path":"%s","createParents":false}
                """.formatted(target.toString().replace("\\", "\\\\"))));

        assertEquals(ToolExecutionStatus.FAILURE, result.status());
        assertTrue(result.output().contains("NoSuchFileException"));
    }
}

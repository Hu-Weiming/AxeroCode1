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

class WriteFileToolTest {

    @Test
    void executeCreatesFileAndParentDirectoriesByDefault(@TempDir Path tempDir) throws Exception {
        Path target = tempDir.resolve("src").resolve("HelloWorld.java");
        WriteFileTool tool = new WriteFileTool();

        ToolExecutionResult result = tool.execute(ToolCall.create("write_file", """
                {"path":"%s","content":"public class HelloWorld {}"}
                """.formatted(target.toString().replace("\\", "\\\\"))));

        assertEquals(ToolExecutionStatus.SUCCESS, result.status());
        assertEquals("public class HelloWorld {}", Files.readString(target));
        assertEquals("Wrote file: " + target.toAbsolutePath(), result.output());
    }

    @Test
    void executeFailsWhenFileExistsAndOverwriteIsFalse(@TempDir Path tempDir) throws Exception {
        Path target = tempDir.resolve("demo.txt");
        Files.writeString(target, "original");
        WriteFileTool tool = new WriteFileTool();

        ToolExecutionResult result = tool.execute(ToolCall.create("write_file", """
                {"path":"%s","content":"updated","overwrite":false}
                """.formatted(target.toString().replace("\\", "\\\\"))));

        assertEquals(ToolExecutionStatus.FAILURE, result.status());
        assertEquals("File already exists and overwrite=false: " + target.toAbsolutePath(), result.output());
        assertEquals("original", Files.readString(target));
    }

    @Test
    void executeAppendsContentWhenAppendIsTrue(@TempDir Path tempDir) throws Exception {
        Path target = tempDir.resolve("demo.txt");
        Files.writeString(target, "hello");
        WriteFileTool tool = new WriteFileTool();

        ToolExecutionResult result = tool.execute(ToolCall.create("write_file", """
                {"path":"%s","content":" world","append":true}
                """.formatted(target.toString().replace("\\", "\\\\"))));

        assertEquals(ToolExecutionStatus.SUCCESS, result.status());
        assertEquals("hello world", Files.readString(target));
        assertEquals("Appended file: " + target.toAbsolutePath(), result.output());
    }

    @Test
    void executeFailsWhenRequiredContentIsMissing(@TempDir Path tempDir) {
        Path target = tempDir.resolve("demo.txt");
        WriteFileTool tool = new WriteFileTool();

        ToolExecutionResult result = tool.execute(ToolCall.create("write_file", """
                {"path":"%s"}
                """.formatted(target.toString().replace("\\", "\\\\"))));

        assertEquals(ToolExecutionStatus.FAILURE, result.status());
        assertEquals("Missing required string argument: content", result.output());
    }
}

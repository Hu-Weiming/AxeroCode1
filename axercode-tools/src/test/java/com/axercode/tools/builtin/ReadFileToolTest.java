package com.axercode.tools.builtin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.axercode.core.tool.ToolCall;
import com.axercode.core.tool.ToolExecutionResult;
import com.axercode.core.tool.ToolExecutionStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReadFileToolTest {

    @Test
    void executeReadsFileContent(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("demo.txt");
        Files.writeString(file, "hello from AxerCode");
        ReadFileTool tool = new ReadFileTool();

        ToolExecutionResult result = tool.execute(ToolCall.create("read_file", """
                {"path":"%s"}
                """.formatted(file.toString().replace("\\", "\\\\"))));

        assertEquals(ToolExecutionStatus.SUCCESS, result.status());
        assertEquals("hello from AxerCode", result.output());
    }

    @Test
    void executeReturnsFailureWhenPathIsMissing() {
        ReadFileTool tool = new ReadFileTool();

        ToolExecutionResult result = tool.execute(ToolCall.create("read_file", "{}"));

        assertEquals(ToolExecutionStatus.FAILURE, result.status());
        assertEquals("Missing required string argument: path", result.output());
    }
}

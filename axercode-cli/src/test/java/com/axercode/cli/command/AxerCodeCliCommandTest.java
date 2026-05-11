package com.axercode.cli.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axercode.cli.config.AxerCodeShellProperties;
import com.axercode.cli.service.CliChatService;
import com.axercode.cli.service.CliChatTurn;
import com.axercode.cli.service.InteractiveShellService;
import com.axercode.core.session.SessionContext;
import com.axercode.core.tool.ToolCall;
import com.axercode.core.tool.ToolExecutionResult;
import com.axercode.provider.api.ProviderException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class AxerCodeCliCommandTest {

    @Test
    void executePrintsProviderReplyToStdout() {
        CliChatService service = new StubCliChatService("AxerCode ready");
        AxerCodeCliCommand command = new AxerCodeCliCommand(service, new StubInteractiveShellService());
        CommandLine commandLine = new CommandLine(command);
        StringWriter stdout = new StringWriter();
        StringWriter stderr = new StringWriter();
        commandLine.setOut(new PrintWriter(stdout));
        commandLine.setErr(new PrintWriter(stderr));

        int exitCode = commandLine.execute("--prompt", "hello");

        assertEquals(0, exitCode);
        assertTrue(stdout.toString().contains("AxerCode ready"));
        assertEquals("", stderr.toString());
    }

    @Test
    void executePrintsToolFeedbackBeforeReplyInPromptMode() {
        CliChatService service = new StubCliChatService(
                "AxerCode ready",
                List.of(ToolExecutionResult.success(
                        ToolCall.create("read_file", "{\"path\":\"README.md\"}"),
                        "README content"
                ))
        );
        AxerCodeCliCommand command = new AxerCodeCliCommand(service, new StubInteractiveShellService());
        CommandLine commandLine = new CommandLine(command);
        StringWriter stdout = new StringWriter();
        StringWriter stderr = new StringWriter();
        commandLine.setOut(new PrintWriter(stdout));
        commandLine.setErr(new PrintWriter(stderr));

        int exitCode = commandLine.execute("--prompt", "hello");

        assertEquals(0, exitCode);
        assertTrue(stdout.toString().contains("[tool] read_file SUCCESS"));
        assertTrue(stdout.toString().contains("README content"));
        assertTrue(stdout.toString().contains("AxerCode ready"));
        assertTrue(stdout.toString().indexOf("[tool] read_file SUCCESS") < stdout.toString().indexOf("AxerCode ready"));
        assertEquals("", stderr.toString());
    }

    @Test
    void executeStreamsPromptReplyWithoutDuplicatingFinalLine() {
        CliChatService service = new StreamingStubCliChatService("AxerCode stream");
        AxerCodeCliCommand command = new AxerCodeCliCommand(service, new StubInteractiveShellService());
        CommandLine commandLine = new CommandLine(command);
        StringWriter stdout = new StringWriter();
        StringWriter stderr = new StringWriter();
        commandLine.setOut(new PrintWriter(stdout));
        commandLine.setErr(new PrintWriter(stderr));

        int exitCode = commandLine.execute("--prompt", "hello");

        assertEquals(0, exitCode);
        assertEquals(1, countOccurrences(stdout.toString(), "AxerCode stream"));
        assertEquals("", stderr.toString());
    }

    @Test
    void executePrintsFriendlyProviderErrorsToStderr() {
        CliChatService service = new FailingCliChatService();
        AxerCodeCliCommand command = new AxerCodeCliCommand(service, new StubInteractiveShellService());
        CommandLine commandLine = new CommandLine(command);
        StringWriter stdout = new StringWriter();
        StringWriter stderr = new StringWriter();
        commandLine.setOut(new PrintWriter(stdout));
        commandLine.setErr(new PrintWriter(stderr));

        int exitCode = commandLine.execute("--prompt", "hello");

        assertEquals(1, exitCode);
        assertEquals("", stdout.toString());
        assertTrue(stderr.toString().contains("[AxerCode] ollama/generate failed: Failed to call Ollama /api/chat"));
    }

    @Test
    void executeRunsInteractiveShellWhenRequested() {
        StubInteractiveShellService shellService = new StubInteractiveShellService();
        AxerCodeCliCommand command = new AxerCodeCliCommand(new StubCliChatService("unused"), shellService);
        CommandLine commandLine = new CommandLine(command);
        StringWriter stdout = new StringWriter();
        StringWriter stderr = new StringWriter();
        commandLine.setOut(new PrintWriter(stdout));
        commandLine.setErr(new PrintWriter(stderr));

        int exitCode = commandLine.execute("--interactive");

        assertEquals(0, exitCode);
        assertTrue(shellService.wasInvoked());
        assertEquals("", stderr.toString());
    }

    @Test
    void executeRejectsMissingPromptAndInteractiveMode() {
        AxerCodeCliCommand command = new AxerCodeCliCommand(new StubCliChatService("unused"), new StubInteractiveShellService());
        CommandLine commandLine = new CommandLine(command);
        StringWriter stdout = new StringWriter();
        StringWriter stderr = new StringWriter();
        commandLine.setOut(new PrintWriter(stdout));
        commandLine.setErr(new PrintWriter(stderr));

        int exitCode = commandLine.execute();

        assertEquals(2, exitCode);
        assertTrue(stderr.toString().contains("[AxerCode] Either --prompt or --interactive must be provided."));
    }

    private static class StubCliChatService extends CliChatService {
        private final CliChatTurn turn;

        StubCliChatService(String reply) {
            super(null, null);
            this.turn = new CliChatTurn(SessionContext.start(), reply, List.of());
        }

        StubCliChatService(String reply, List<ToolExecutionResult> toolResults) {
            super(null, null);
            this.turn = new CliChatTurn(SessionContext.start(), reply, toolResults);
        }

        @Override
        public String ask(String prompt, String modelOverride) {
            return turn.reply();
        }

        @Override
        public CliChatTurn askTurn(String prompt, String modelOverride) {
            return turn;
        }

        @Override
        public CliChatTurn askTurnStreaming(String prompt, String modelOverride, Consumer<String> textDeltaConsumer) {
            return turn;
        }
    }

    private static final class StreamingStubCliChatService extends CliChatService {
        private final CliChatTurn turn;

        private StreamingStubCliChatService(String reply) {
            super(null, null);
            this.turn = new CliChatTurn(SessionContext.start(), reply, List.of());
        }

        @Override
        public CliChatTurn askTurn(String prompt, String modelOverride) {
            return turn;
        }

        @Override
        public CliChatTurn askTurnStreaming(String prompt, String modelOverride, Consumer<String> textDeltaConsumer) {
            textDeltaConsumer.accept("Axer");
            textDeltaConsumer.accept("Code stream");
            return turn;
        }
    }

    private static final class FailingCliChatService extends CliChatService {
        FailingCliChatService() {
            super(null, null);
        }

        @Override
        public String ask(String prompt, String modelOverride) {
            throw new ProviderException("ollama", "generate", "Failed to call Ollama /api/chat");
        }

        @Override
        public CliChatTurn askTurn(String prompt, String modelOverride) {
            throw new ProviderException("ollama", "generate", "Failed to call Ollama /api/chat");
        }

        @Override
        public CliChatTurn askTurnStreaming(String prompt, String modelOverride, Consumer<String> textDeltaConsumer) {
            throw new ProviderException("ollama", "generate", "Failed to call Ollama /api/chat");
        }
    }

    private static final class StubInteractiveShellService extends InteractiveShellService {
        private boolean invoked;

        StubInteractiveShellService() {
            super(null, null, new AxerCodeShellProperties());
        }

        @Override
        public int runInteractive(String modelOverride) {
            invoked = true;
            return 0;
        }

        @Override
        public int run(Reader reader, PrintWriter writer, String modelOverride) {
            throw new AssertionError("Interactive mode should use the JLine-backed runInteractive path.");
        }

        boolean wasInvoked() {
            return invoked;
        }
    }

    private static int countOccurrences(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}

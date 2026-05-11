package com.axercode.cli.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axercode.agent.AgentConversationTurn;
import com.axercode.agent.ConversationAgent;
import com.axercode.cli.config.AxerCodeShellProperties;
import com.axercode.cli.config.AxerCodeProviderProperties;
import com.axercode.cli.shell.SlashCommandDispatcher;
import com.axercode.core.session.ConversationMessage;
import com.axercode.core.session.MessageRole;
import com.axercode.core.session.SessionContext;
import com.axercode.core.tool.ToolCall;
import com.axercode.core.tool.ToolExecutionResult;
import com.axercode.storage.sqlite.session.SqliteSessionRepository;
import com.axercode.storage.sqlite.shell.SqliteShellStateRepository;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.Instant;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class InteractiveShellServiceTest {

    @Test
    void runSupportsHelpHistoryNewAndExitCommands() {
        RecordingAgent agent = new RecordingAgent();
        AxerCodeProviderProperties properties = new AxerCodeProviderProperties();
        properties.setDefaultModel("qwen2.5:7b");
        CliChatService chatService = new CliChatService(agent, properties);
        InMemorySessionStore sessionStore = new InMemorySessionStore();
        InteractiveShellService shellService = new InteractiveShellService(chatService, sessionStore);

        StringReader input = new StringReader("""
                /help
                hello
                /history
                /new
                /history
                /exit
                """);
        StringWriter output = new StringWriter();

        int exitCode = shellService.run(input, new PrintWriter(output, true), null);

        assertEquals(0, exitCode);
        assertTrue(output.toString().contains("AxerCode interactive shell started."));
        assertTrue(output.toString().contains("/help"));
        assertTrue(output.toString().contains("/status"));
        assertTrue(output.toString().contains("/checkpoint"));
        assertTrue(output.toString().contains("/diff"));
        assertTrue(output.toString().contains("reply-1"));
        assertTrue(output.toString().contains("USER: hello"));
        assertTrue(output.toString().contains("ASSISTANT: reply-1"));
        assertTrue(output.toString().contains("[AxerCode] Started a new in-memory session."));
        assertTrue(output.toString().contains("[AxerCode] No messages in the current session."));
    }

    @Test
    void runMaintainsMultiTurnHistoryAcrossPrompts() {
        RecordingAgent agent = new RecordingAgent();
        AxerCodeProviderProperties properties = new AxerCodeProviderProperties();
        properties.setDefaultModel("qwen2.5:7b");
        CliChatService chatService = new CliChatService(agent, properties);
        InMemorySessionStore sessionStore = new InMemorySessionStore();
        InteractiveShellService shellService = new InteractiveShellService(chatService, sessionStore);

        StringReader input = new StringReader("""
                hello
                summarize again
                /exit
                """);
        StringWriter output = new StringWriter();

        shellService.run(input, new PrintWriter(output, true), null);

        assertEquals(2, agent.requestedSessions().size());
        assertEquals(0, agent.requestedSessions().get(0).messages().size());
        assertEquals(2, agent.requestedSessions().get(1).messages().size());
        assertEquals("summarize again", agent.prompts().get(1));
        assertTrue(output.toString().contains("reply-1"));
        assertTrue(output.toString().contains("reply-2"));
    }

    @Test
    void runPrintsToolFeedbackBeforeAssistantReply() {
        RecordingAgent agent = new RecordingAgent(List.of(
                ToolExecutionResult.success(ToolCall.create("read_file", "{\"path\":\"README.md\"}"), "README content")
        ));
        AxerCodeProviderProperties properties = new AxerCodeProviderProperties();
        properties.setDefaultModel("qwen2.5:7b");
        CliChatService chatService = new CliChatService(agent, properties);
        InMemorySessionStore sessionStore = new InMemorySessionStore();
        InteractiveShellService shellService = new InteractiveShellService(chatService, sessionStore);

        StringReader input = new StringReader("""
                inspect readme
                /exit
                """);
        StringWriter output = new StringWriter();

        int exitCode = shellService.run(input, new PrintWriter(output, true), null);

        assertEquals(0, exitCode);
        String rendered = output.toString();
        assertTrue(rendered.contains("[tool] read_file SUCCESS"));
        assertTrue(rendered.contains("README content"));
        assertTrue(rendered.contains("reply-1"));
        assertTrue(rendered.indexOf("[tool] read_file SUCCESS") < rendered.indexOf("reply-1"));
    }

    @Test
    void runStreamsAssistantReplyWithoutDuplicatingFinalOutput() {
        StreamingAgent agent = new StreamingAgent();
        AxerCodeProviderProperties properties = new AxerCodeProviderProperties();
        properties.setDefaultModel("qwen2.5:7b");
        CliChatService chatService = new CliChatService(agent, properties);
        InMemorySessionStore sessionStore = new InMemorySessionStore();
        InteractiveShellService shellService = new InteractiveShellService(chatService, sessionStore);

        StringReader input = new StringReader("""
                hello
                /exit
                """);
        StringWriter output = new StringWriter();

        int exitCode = shellService.run(input, new PrintWriter(output, true), null);

        assertEquals(0, exitCode);
        assertEquals(1, countOccurrences(output.toString(), "stream-reply"));
    }

    @Test
    void runSupportsAdvancedSlashCommands(@TempDir Path tempDir) {
        RecordingAgent agent = new RecordingAgent();
        AxerCodeProviderProperties properties = new AxerCodeProviderProperties();
        properties.setDefaultModel("qwen2.5:7b");
        CliChatService chatService = new CliChatService(agent, properties);
        InMemorySessionStore sessionStore = new InMemorySessionStore();
        InteractiveShellService shellService = new InteractiveShellService(chatService, sessionStore);

        StringReader input = new StringReader(String.join(System.lineSeparator(),
                "/status",
                "/focus " + tempDir,
                "hello",
                "/checkpoint alpha",
                "/new",
                "/restore alpha",
                "/history",
                "/checkpoints",
                "/exit",
                ""
        ));
        StringWriter output = new StringWriter();

        int exitCode = shellService.run(input, new PrintWriter(output, true), null);

        assertEquals(0, exitCode);
        String rendered = output.toString();
        assertTrue(rendered.contains("Model: qwen2.5:7b"));
        assertTrue(rendered.contains("Focus: <none>"));
        assertTrue(rendered.contains("[AxerCode] Focus set to: " + tempDir.toAbsolutePath()));
        assertTrue(rendered.contains("[AxerCode] Saved checkpoint 'alpha' (2 messages)."));
        assertTrue(rendered.contains("[AxerCode] Restored checkpoint 'alpha' (2 messages)."));
        assertTrue(rendered.contains("USER: hello"));
        assertTrue(rendered.contains("ASSISTANT: reply-1"));
        assertTrue(rendered.contains("alpha"));
    }

    @Test
    void runInjectsFocusIntoLaterPromptTurns(@TempDir Path tempDir) {
        RecordingAgent agent = new RecordingAgent();
        AxerCodeProviderProperties properties = new AxerCodeProviderProperties();
        properties.setDefaultModel("qwen2.5:7b");
        InMemoryShellStateStore shellStateStore = new InMemoryShellStateStore();
        CliChatService chatService = new CliChatService(agent, properties, shellStateStore);
        InMemorySessionStore sessionStore = new InMemorySessionStore();
        InteractiveShellService shellService = new InteractiveShellService(
                chatService,
                sessionStore,
                shellStateStore,
                new AxerCodeShellProperties()
        );

        StringReader input = new StringReader(String.join(System.lineSeparator(),
                "/focus " + tempDir,
                "hello",
                "/exit",
                ""
        ));
        StringWriter output = new StringWriter();

        int exitCode = shellService.run(input, new PrintWriter(output, true), null);

        assertEquals(0, exitCode);
        assertEquals(1, agent.requestedSessions().size());
        assertEquals(MessageRole.SYSTEM, agent.requestedSessions().getFirst().messages().getFirst().role());
        assertTrue(agent.requestedSessions().getFirst().messages().getFirst().content().contains(tempDir.toAbsolutePath().toString()));
        assertEquals(2, sessionStore.currentSession().messages().size());
        assertEquals(MessageRole.USER, sessionStore.currentSession().messages().getFirst().role());
        assertEquals(MessageRole.ASSISTANT, sessionStore.currentSession().messages().get(1).role());
    }

    @Test
    void runWithTerminalPersistsHistoryAndKeepsShellBehavior(@TempDir Path tempDir) throws Exception {
        RecordingAgent agent = new RecordingAgent();
        AxerCodeProviderProperties properties = new AxerCodeProviderProperties();
        properties.setDefaultModel("qwen2.5:7b");
        CliChatService chatService = new CliChatService(agent, properties);
        InMemorySessionStore sessionStore = new InMemorySessionStore();
        AxerCodeShellProperties shellProperties = new AxerCodeShellProperties();
        Path historyPath = tempDir.resolve("cli.history");
        shellProperties.setHistoryFile(historyPath.toString());
        InteractiveShellService shellService = new InteractiveShellService(chatService, sessionStore, shellProperties);

        ByteArrayInputStream input = new ByteArrayInputStream("""
                hello
                /history
                /exit
                """.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (Terminal terminal = TerminalBuilder.builder()
                .system(false)
                .streams(input, output)
                .dumb(true)
                .encoding(StandardCharsets.UTF_8)
                .build()) {
            int exitCode = shellService.run(terminal, null);
            assertEquals(0, exitCode);
        }

        String terminalOutput = output.toString(StandardCharsets.UTF_8);
        assertTrue(terminalOutput.contains("AxerCode interactive shell started."));
        assertTrue(terminalOutput.contains("reply-1"));
        assertTrue(terminalOutput.contains("USER: hello"));
        assertTrue(terminalOutput.contains("ASSISTANT: reply-1"));
        assertTrue(Files.exists(historyPath));
        assertFalse(Files.readString(historyPath).isBlank());
    }

    @Test
    void springCanInstantiateInteractiveShellServiceWithShellProperties() {
        RecordingAgent agent = new RecordingAgent();
        AxerCodeProviderProperties properties = new AxerCodeProviderProperties();
        properties.setDefaultModel("qwen2.5:7b");
        AxerCodeShellProperties shellProperties = new AxerCodeShellProperties();

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(CliChatService.class, () -> new CliChatService(agent, properties));
            context.registerBean(InMemorySessionStore.class, InMemorySessionStore::new);
            context.registerBean(ShellStateStore.class, InMemoryShellStateStore::new);
            context.registerBean(AxerCodeShellProperties.class, () -> shellProperties);
            context.registerBean(InteractiveShellService.class);

            context.refresh();

            assertTrue(context.containsBeanDefinition("interactiveShellService"));
        }
    }

    @Test
    void advancedSlashCommandsPersistAcrossShellRestarts(@TempDir Path tempDir) {
        RecordingAgent agent = new RecordingAgent();
        AxerCodeProviderProperties properties = new AxerCodeProviderProperties();
        properties.setDefaultModel("qwen2.5:7b");
        CliChatService chatService = new CliChatService(agent, properties);
        Path databaseFile = tempDir.resolve("axercode.db");

        InteractiveShellService firstShell = new InteractiveShellService(
                chatService,
                new SqliteBackedSessionStore(new SqliteSessionRepository(databaseFile)),
                new SqliteBackedShellStateStore(new SqliteShellStateRepository(new SqliteSessionRepository(databaseFile))),
                new AxerCodeShellProperties()
        );

        firstShell.run(
                new StringReader(String.join(System.lineSeparator(),
                        "/focus " + tempDir,
                        "hello",
                        "/checkpoint alpha",
                        "/exit",
                        ""
                )),
                new PrintWriter(new StringWriter(), true),
                null
        );

        InteractiveShellService secondShell = new InteractiveShellService(
                chatService,
                new SqliteBackedSessionStore(new SqliteSessionRepository(databaseFile)),
                new SqliteBackedShellStateStore(new SqliteShellStateRepository(new SqliteSessionRepository(databaseFile))),
                new AxerCodeShellProperties()
        );
        StringWriter output = new StringWriter();

        int exitCode = secondShell.run(
                new StringReader(String.join(System.lineSeparator(),
                        "/status",
                        "/checkpoints",
                        "/restore alpha",
                        "/history",
                        "/exit",
                        ""
                )),
                new PrintWriter(output, true),
                null
        );

        assertEquals(0, exitCode);
        String rendered = output.toString();
        assertTrue(rendered.contains("Focus: " + tempDir.toAbsolutePath()));
        assertTrue(rendered.contains("alpha"));
        assertTrue(rendered.contains("[AxerCode] Restored checkpoint 'alpha' (2 messages)."));
        assertTrue(rendered.contains("USER: hello"));
        assertTrue(rendered.contains("ASSISTANT: reply-1"));
    }

    private static final class RecordingAgent implements ConversationAgent {
        private final List<ToolExecutionResult> toolResults;
        private final List<SessionContext> requestedSessions = new ArrayList<>();
        private final List<String> prompts = new ArrayList<>();

        private RecordingAgent() {
            this(List.of());
        }

        private RecordingAgent(List<ToolExecutionResult> toolResults) {
            this.toolResults = toolResults;
        }

        @Override
        public AgentConversationTurn continueConversation(SessionContext sessionContext, String prompt, String model) {
            requestedSessions.add(sessionContext);
            prompts.add(prompt);

            SessionContext updated = sessionContext
                    .append(ConversationMessage.user(prompt))
                    .append(new ConversationMessage(
                            UUID.randomUUID(),
                    MessageRole.ASSISTANT,
                    "reply-" + prompts.size(),
                    Instant.now()
                    ));
            return new AgentConversationTurn(updated, "reply-" + prompts.size(), toolResults);
        }

        List<SessionContext> requestedSessions() {
            return requestedSessions;
        }

        List<String> prompts() {
            return prompts;
        }
    }

    private static final class StreamingAgent implements ConversationAgent {
        @Override
        public AgentConversationTurn continueConversation(SessionContext sessionContext, String prompt, String model) {
            throw new AssertionError("streaming test should use continueConversationStreaming");
        }

        @Override
        public AgentConversationTurn continueConversationStreaming(
                SessionContext sessionContext,
                String prompt,
                String model,
                Consumer<String> textDeltaConsumer
        ) {
            textDeltaConsumer.accept("stream-");
            textDeltaConsumer.accept("reply");
            SessionContext updated = sessionContext
                    .append(ConversationMessage.user(prompt))
                    .append(new ConversationMessage(
                            UUID.randomUUID(),
                            MessageRole.ASSISTANT,
                            "stream-reply",
                            Instant.now()
                    ));
            return new AgentConversationTurn(updated, "stream-reply", List.of());
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

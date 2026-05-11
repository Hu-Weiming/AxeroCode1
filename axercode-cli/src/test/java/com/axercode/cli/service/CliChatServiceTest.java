package com.axercode.cli.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.axercode.agent.AgentConversationTurn;
import com.axercode.agent.ConversationAgent;
import com.axercode.cli.config.AxerCodeProviderProperties;
import com.axercode.core.session.ConversationMessage;
import com.axercode.core.session.MessageRole;
import com.axercode.core.session.SessionContext;
import com.axercode.core.tool.ToolCall;
import com.axercode.core.tool.ToolExecutionResult;
import java.time.Instant;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class CliChatServiceTest {

    @Test
    void askDelegatesToAgentWithDefaultModel() {
        RecordingAgent agent = new RecordingAgent();
        AxerCodeProviderProperties properties = new AxerCodeProviderProperties();
        properties.setDefaultModel("qwen2.5:7b");
        CliChatService service = new CliChatService(agent, properties);

        String reply = service.ask("Summarize the module graph", null);

        assertEquals("Summarize the module graph", agent.prompt);
        assertEquals("qwen2.5:7b", agent.model);
        assertEquals("agent-reply", reply);
    }

    @Test
    void askUsesModelOverrideWhenProvided() {
        RecordingAgent agent = new RecordingAgent();
        AxerCodeProviderProperties properties = new AxerCodeProviderProperties();
        properties.setDefaultModel("qwen2.5:7b");
        CliChatService service = new CliChatService(agent, properties);

        service.ask("hello", "custom-model");

        assertEquals("custom-model", agent.model);
    }

    @Test
    void continueConversationReturnsUpdatedSessionFromAgent() {
        RecordingAgent agent = new RecordingAgent(List.of(
                ToolExecutionResult.success(ToolCall.create("read_file", "{\"path\":\"README.md\"}"), "README content")
        ));
        AxerCodeProviderProperties properties = new AxerCodeProviderProperties();
        properties.setDefaultModel("qwen2.5:7b");
        CliChatService service = new CliChatService(agent, properties);

        SessionContext existingContext = SessionContext.start()
                .append(ConversationMessage.user("What did Step 10 do?"))
                .append(new ConversationMessage(
                        UUID.randomUUID(),
                        MessageRole.ASSISTANT,
                        "It added the tool framework.",
                        Instant.now()
                ));

        CliChatTurn turn = service.continueConversation(existingContext, "Summarize it again", null);

        assertEquals(existingContext.sessionId(), agent.sessionContext.sessionId());
        assertEquals("Summarize it again", agent.prompt);
        assertEquals("agent-reply", turn.reply());
        assertEquals(3, turn.sessionContext().messages().size());
        assertEquals("agent-reply", turn.sessionContext().messages().getLast().content());
        assertEquals(1, turn.toolResults().size());
        assertEquals("read_file", turn.toolResults().getFirst().toolCall().name());
    }

    @Test
    void continueConversationPrependsFocusSystemMessageBeforeAgentCall() {
        RecordingAgent agent = new RecordingAgent();
        AxerCodeProviderProperties properties = new AxerCodeProviderProperties();
        properties.setDefaultModel("qwen2.5:7b");
        InMemoryShellStateStore shellStateStore = new InMemoryShellStateStore();
        shellStateStore.setFocusPath(Path.of("D:/AeroCode1"));
        CliChatService service = new CliChatService(agent, properties, shellStateStore);

        service.continueConversation(SessionContext.start(), "inspect current project", null);

        assertEquals(MessageRole.SYSTEM, agent.sessionContext.messages().getFirst().role());
        assertEquals(
                "Current focus path: D:\\AeroCode1. Prefer this path when resolving relative project references for this turn.",
                agent.sessionContext.messages().getFirst().content()
        );
    }

    @Test
    void continueConversationPrependsActiveCheckpointSystemMessageBeforeAgentCall() {
        RecordingAgent agent = new RecordingAgent();
        AxerCodeProviderProperties properties = new AxerCodeProviderProperties();
        properties.setDefaultModel("qwen2.5:7b");
        InMemoryShellStateStore shellStateStore = new InMemoryShellStateStore();
        shellStateStore.saveCheckpoint("alpha", SessionContext.start().append(ConversationMessage.user("baseline")));
        CliChatService service = new CliChatService(agent, properties, shellStateStore);

        service.continueConversation(SessionContext.start(), "continue from there", null);

        assertEquals(MessageRole.SYSTEM, agent.sessionContext.messages().getFirst().role());
        assertEquals(
                "Active checkpoint: alpha. Treat the current session as working from this named rollback point when the user refers to restoring, comparing, or continuing from a checkpoint.",
                agent.sessionContext.messages().getFirst().content()
        );
    }

    @Test
    void continueConversationPrependsPlanModeSystemMessageBeforeAgentCall() {
        RecordingAgent agent = new RecordingAgent();
        AxerCodeProviderProperties properties = new AxerCodeProviderProperties();
        properties.setDefaultModel("qwen2.5:7b");
        InMemoryShellStateStore shellStateStore = new InMemoryShellStateStore();
        shellStateStore.setPlanModeEnabled(true);
        CliChatService service = new CliChatService(agent, properties, shellStateStore);

        service.continueConversation(SessionContext.start(), "help me approach this", null);

        assertEquals(MessageRole.SYSTEM, agent.sessionContext.messages().getFirst().role());
        assertEquals(
                "Plan mode is enabled. Respond with a concise phased or numbered plan, emphasize analysis and next steps, and do not imply the work has already been executed.",
                agent.sessionContext.messages().getFirst().content()
        );
    }

    @Test
    void continueConversationPrependsActiveBranchSystemMessageBeforeAgentCall() {
        RecordingAgent agent = new RecordingAgent();
        AxerCodeProviderProperties properties = new AxerCodeProviderProperties();
        properties.setDefaultModel("qwen2.5:7b");
        InMemoryShellStateStore shellStateStore = new InMemoryShellStateStore();
        SessionContext branched = SessionContext.start().append(ConversationMessage.user("hello"));
        shellStateStore.saveBranch("feature-a", branched);
        CliChatService service = new CliChatService(agent, properties, shellStateStore);

        service.continueConversation(SessionContext.start(), "continue here", null);

        assertEquals(MessageRole.SYSTEM, agent.sessionContext.messages().getFirst().role());
        assertEquals(
                "Current session branch: feature-a. Treat this conversation as working on that named branch unless the user explicitly switches branches.",
                agent.sessionContext.messages().getFirst().content()
        );
    }

    @Test
    void continueConversationDoesNotPersistTemporaryFocusSystemMessage() {
        RecordingAgent agent = new RecordingAgent();
        AxerCodeProviderProperties properties = new AxerCodeProviderProperties();
        properties.setDefaultModel("qwen2.5:7b");
        InMemoryShellStateStore shellStateStore = new InMemoryShellStateStore();
        shellStateStore.setFocusPath(Path.of("D:/AeroCode1"));
        CliChatService service = new CliChatService(agent, properties, shellStateStore);

        CliChatTurn turn = service.continueConversation(SessionContext.start(), "inspect current project", null);

        assertEquals(1, turn.sessionContext().messages().size());
        assertEquals(MessageRole.ASSISTANT, turn.sessionContext().messages().getFirst().role());
        assertEquals("agent-reply", turn.sessionContext().messages().getFirst().content());
    }

    @Test
    void continueConversationDoesNotPersistTemporaryCheckpointSystemMessage() {
        RecordingAgent agent = new RecordingAgent();
        AxerCodeProviderProperties properties = new AxerCodeProviderProperties();
        properties.setDefaultModel("qwen2.5:7b");
        InMemoryShellStateStore shellStateStore = new InMemoryShellStateStore();
        shellStateStore.saveCheckpoint("alpha", SessionContext.start().append(ConversationMessage.user("baseline")));
        CliChatService service = new CliChatService(agent, properties, shellStateStore);

        CliChatTurn turn = service.continueConversation(SessionContext.start(), "continue from there", null);

        assertEquals(1, turn.sessionContext().messages().size());
        assertEquals(MessageRole.ASSISTANT, turn.sessionContext().messages().getFirst().role());
        assertEquals("agent-reply", turn.sessionContext().messages().getFirst().content());
    }

    @Test
    void continueConversationDoesNotPersistTemporaryPlanModeSystemMessage() {
        RecordingAgent agent = new RecordingAgent();
        AxerCodeProviderProperties properties = new AxerCodeProviderProperties();
        properties.setDefaultModel("qwen2.5:7b");
        InMemoryShellStateStore shellStateStore = new InMemoryShellStateStore();
        shellStateStore.setPlanModeEnabled(true);
        CliChatService service = new CliChatService(agent, properties, shellStateStore);

        CliChatTurn turn = service.continueConversation(SessionContext.start(), "help me approach this", null);

        assertEquals(1, turn.sessionContext().messages().size());
        assertEquals(MessageRole.ASSISTANT, turn.sessionContext().messages().getFirst().role());
        assertEquals("agent-reply", turn.sessionContext().messages().getFirst().content());
    }

    @Test
    void continueConversationDoesNotPersistTemporaryBranchSystemMessage() {
        RecordingAgent agent = new RecordingAgent();
        AxerCodeProviderProperties properties = new AxerCodeProviderProperties();
        properties.setDefaultModel("qwen2.5:7b");
        InMemoryShellStateStore shellStateStore = new InMemoryShellStateStore();
        SessionContext branched = SessionContext.start().append(ConversationMessage.user("hello"));
        shellStateStore.saveBranch("feature-a", branched);
        CliChatService service = new CliChatService(agent, properties, shellStateStore);

        CliChatTurn turn = service.continueConversation(SessionContext.start(), "continue here", null);

        assertEquals(1, turn.sessionContext().messages().size());
        assertEquals(MessageRole.ASSISTANT, turn.sessionContext().messages().getFirst().role());
        assertEquals("agent-reply", turn.sessionContext().messages().getFirst().content());
    }

    @Test
    void askRejectsBlankPrompt() {
        AxerCodeProviderProperties properties = new AxerCodeProviderProperties();
        properties.setDefaultModel("qwen2.5:7b");
        CliChatService service = new CliChatService(new RecordingAgent(), properties);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.ask(" ", null)
        );

        assertEquals("prompt must not be blank", exception.getMessage());
    }

    @Test
    void continueConversationStreamingPrependsFocusButDoesNotPersistIt() {
        RecordingAgent agent = new RecordingAgent();
        AxerCodeProviderProperties properties = new AxerCodeProviderProperties();
        properties.setDefaultModel("qwen2.5:7b");
        InMemoryShellStateStore shellStateStore = new InMemoryShellStateStore();
        shellStateStore.setFocusPath(Path.of("D:/AeroCode1"));
        CliChatService service = new CliChatService(agent, properties, shellStateStore);
        StringBuilder streamed = new StringBuilder();

        CliChatTurn turn = service.continueConversationStreaming(
                SessionContext.start(),
                "stream this",
                null,
                streamed::append
        );

        assertEquals("agent-stream", streamed.toString());
        assertEquals(MessageRole.SYSTEM, agent.sessionContext.messages().getFirst().role());
        assertEquals(1, turn.sessionContext().messages().size());
        assertEquals("agent-stream", turn.reply());
    }

    private static final class RecordingAgent implements ConversationAgent {
        private final List<ToolExecutionResult> toolResults;
        private SessionContext sessionContext;
        private String prompt;
        private String model;

        private RecordingAgent() {
            this(List.of());
        }

        private RecordingAgent(List<ToolExecutionResult> toolResults) {
            this.toolResults = toolResults;
        }

        @Override
        public AgentConversationTurn continueConversation(SessionContext sessionContext, String prompt, String model) {
            this.sessionContext = sessionContext;
            this.prompt = prompt;
            this.model = model;

            SessionContext updatedSession = sessionContext.append(new ConversationMessage(
                    UUID.randomUUID(),
                    MessageRole.ASSISTANT,
                    "agent-reply",
                    Instant.now()
            ));
            return new AgentConversationTurn(updatedSession, "agent-reply", toolResults);
        }

        @Override
        public AgentConversationTurn continueConversationStreaming(
                SessionContext sessionContext,
                String prompt,
                String model,
                Consumer<String> textDeltaConsumer
        ) {
            textDeltaConsumer.accept("agent-stream");
            this.sessionContext = sessionContext;
            this.prompt = prompt;
            this.model = model;

            SessionContext updatedSession = sessionContext.append(new ConversationMessage(
                    UUID.randomUUID(),
                    MessageRole.ASSISTANT,
                    "agent-stream",
                    Instant.now()
            ));
            return new AgentConversationTurn(updatedSession, "agent-stream", toolResults);
        }
    }
}

package com.axercode.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.axercode.agent.AgentConversationTurn;
import com.axercode.agent.ConversationAgent;
import com.axercode.agent.TurnInterruptionSource;
import com.axercode.core.session.ConversationMessage;
import com.axercode.core.session.MessageRole;
import com.axercode.core.session.SessionContext;
import com.axercode.core.session.SessionId;
import com.axercode.storage.sqlite.session.SqliteSessionRepository;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ServerConversationServiceTest {

    @Test
    void chatStartsNewSessionWhenSessionIdMissing(@TempDir Path tempDir) {
        RecordingAgent agent = new RecordingAgent();
        SqliteSessionRepository repository = new SqliteSessionRepository(tempDir.resolve("server.db"));
        ServerConversationService service = new ServerConversationService(agent, repository, "qwen2.5:7b");

        ServerConversationTurn turn = service.chat("hello", null, null);

        assertEquals("ollama::qwen2.5:7b", agent.model);
        assertEquals("ollama", turn.provider());
        assertEquals("qwen2.5:7b", turn.model());
        assertEquals(2, turn.sessionContext().messages().size());
        assertEquals("hello", turn.sessionContext().messages().getFirst().content());
        assertEquals("agent-reply", repository.loadSession(turn.sessionContext().sessionId()).orElseThrow().messages().getLast().content());
    }

    @Test
    void chatLoadsExistingSessionWhenSessionIdProvided(@TempDir Path tempDir) {
        RecordingAgent agent = new RecordingAgent();
        SqliteSessionRepository repository = new SqliteSessionRepository(tempDir.resolve("server.db"));
        SessionContext existing = SessionContext.start()
                .append(message(MessageRole.USER, "saved", "2026-04-18T20:00:00Z"));
        repository.saveSession(existing);
        ServerConversationService service = new ServerConversationService(agent, repository, "qwen2.5:7b");

        ServerConversationTurn turn = service.chat("continue", existing.sessionId().toString(), "custom-model");

        assertEquals(existing.sessionId(), agent.sessionContext.sessionId());
        assertEquals("ollama::custom-model", agent.model);
        assertEquals("ollama", turn.provider());
        assertEquals("custom-model", turn.model());
        assertEquals(3, turn.sessionContext().messages().size());
    }

    @Test
    void chatInjectsPlanModeContextWithoutPersistingIt(@TempDir Path tempDir) {
        RecordingAgent agent = new RecordingAgent();
        SqliteSessionRepository repository = new SqliteSessionRepository(tempDir.resolve("server.db"));
        ServerConversationService service = new ServerConversationService(agent, repository, "qwen2.5:7b");
        service.setPlanModeEnabled(true);

        ServerConversationTurn turn = service.chat("hello", null, null);

        assertEquals("Plan mode is enabled. Respond with a concise phased or numbered plan, emphasize analysis and next steps, and do not imply the work has already been executed.",
                agent.sessionContext.messages().getFirst().content());
        assertEquals(2, turn.sessionContext().messages().size());
        assertEquals("hello", turn.sessionContext().messages().getFirst().content());
    }

    @Test
    void streamChatForwardsDeltasAndPersistsUpdatedSession(@TempDir Path tempDir) {
        StreamingAgent agent = new StreamingAgent();
        SqliteSessionRepository repository = new SqliteSessionRepository(tempDir.resolve("server.db"));
        ServerConversationService service = new ServerConversationService(agent, repository, "qwen2.5:7b");
        StringBuilder streamed = new StringBuilder();

        ServerConversationTurn turn = service.chatStreaming("hello", null, null, streamed::append);

        assertEquals("stream-reply", streamed.toString());
        assertEquals("stream-reply", turn.reply());
        assertEquals("stream-reply", repository.loadSession(turn.sessionContext().sessionId()).orElseThrow().messages().getLast().content());
    }

    @Test
    void queueBtwMessagesFeedsThePreparedStreamingTurn(@TempDir Path tempDir) {
        InterruptibleStreamingAgent agent = new InterruptibleStreamingAgent();
        SqliteSessionRepository repository = new SqliteSessionRepository(tempDir.resolve("server.db"));
        ServerConversationService service = new ServerConversationService(agent, repository, "qwen2.5:7b");
        PreparedStreamingSession preparedSession = service.prepareStreamingSession(null, null);

        assertEquals(1, service.queueBtwMessage(preparedSession.sessionId(), "one more thing"));

        try {
            ServerConversationTurn turn = service.chatStreaming(preparedSession, "hello", delta -> {
            });

            assertEquals(List.of("one more thing"), agent.pendingMessages);
            assertEquals("interrupt-aware-reply", turn.reply());
        } finally {
            service.finishStreamingSession(preparedSession.sessionId());
        }
    }

    @Test
    void loadSessionRejectsUnknownSession(@TempDir Path tempDir) {
        SqliteSessionRepository repository = new SqliteSessionRepository(tempDir.resolve("server.db"));
        ServerConversationService service = new ServerConversationService(new RecordingAgent(), repository, "qwen2.5:7b");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.loadSession("7e4567e0-e89b-12d3-a456-426614174000")
        );

        assertEquals("Unknown session: 7e4567e0-e89b-12d3-a456-426614174000", exception.getMessage());
    }

    @Test
    void loadSessionPreservesPerAssistantModelHistoryAcrossModelSwitches(@TempDir Path tempDir) {
        SqliteSessionRepository repository = new SqliteSessionRepository(tempDir.resolve("server.db"));
        SessionContext session = SessionContext.start();

        ConversationMessage firstUser = message(MessageRole.USER, "Use Claude", "2026-04-19T09:00:00Z");
        ConversationMessage firstAssistant = message(MessageRole.ASSISTANT, "Claude reply", "2026-04-19T09:00:01Z");
        session = session.append(firstUser).append(firstAssistant);
        repository.saveSession(session, "anthropic", "claude-3-5-sonnet-latest");

        ConversationMessage secondUser = message(MessageRole.USER, "Now switch to qwen", "2026-04-19T09:01:00Z");
        ConversationMessage secondAssistant = message(MessageRole.ASSISTANT, "Qwen reply", "2026-04-19T09:01:01Z");
        session = session.append(secondUser).append(secondAssistant);
        repository.saveSession(session, "ollama", "qwen2.5:14b");

        ServerConversationService service = new ServerConversationService(new RecordingAgent(), repository, "qwen2.5:7b");

        ServerStoredSession loaded = service.loadSession(session.sessionId().toString());

        assertEquals("ollama", loaded.provider());
        assertEquals("qwen2.5:14b", loaded.model());
        assertEquals(Map.of(
                firstAssistant.id(), "claude-3-5-sonnet-latest",
                secondAssistant.id(), "qwen2.5:14b"
        ), loaded.messageModels());
    }

    @Test
    void listSessionsReturnsPersistedSummaries(@TempDir Path tempDir) throws Exception {
        SqliteSessionRepository repository = new SqliteSessionRepository(tempDir.resolve("server.db"));
        SessionContext firstSession = SessionContext.start()
                .append(message(MessageRole.USER, "Sketch the new layout", "2026-04-18T20:10:00Z"))
                .append(message(MessageRole.ASSISTANT, "Drafted the layout notes.", "2026-04-18T20:10:01Z"));
        SessionContext secondSession = SessionContext.start()
                .append(message(MessageRole.USER, "Wire up the session list", "2026-04-18T20:11:00Z"))
                .append(message(MessageRole.ASSISTANT, "Connected the sidebar state.", "2026-04-18T20:11:01Z"));
        repository.saveSession(firstSession);
        Thread.sleep(25L);
        repository.saveSession(secondSession);
        ServerConversationService service = new ServerConversationService(new RecordingAgent(), repository, "qwen2.5:7b");

        List<ServerSessionSummary> sessions = service.listSessions(10);

        assertEquals(2, sessions.size());
        assertEquals(secondSession.sessionId().toString(), sessions.get(0).sessionId());
        assertEquals("Wire up the session list", sessions.get(0).title());
        assertEquals("Connected the sidebar state.", sessions.get(0).preview());
    }

    private static ConversationMessage message(MessageRole role, String content, String instant) {
        return new ConversationMessage(UUID.randomUUID(), role, content, Instant.parse(instant));
    }

    private static final class RecordingAgent implements ConversationAgent {
        private SessionContext sessionContext;
        private String model;

        @Override
        public AgentConversationTurn continueConversation(SessionContext sessionContext, String prompt, String model) {
            this.sessionContext = sessionContext;
            this.model = model;
            SessionContext updated = sessionContext
                    .append(ConversationMessage.user(prompt))
                    .append(new ConversationMessage(UUID.randomUUID(), MessageRole.ASSISTANT, "agent-reply", Instant.now()));
            return new AgentConversationTurn(updated, "agent-reply", List.of());
        }
    }

    private static final class StreamingAgent implements ConversationAgent {
        @Override
        public AgentConversationTurn continueConversation(SessionContext sessionContext, String prompt, String model) {
            throw new AssertionError("streaming path expected");
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
                    .append(new ConversationMessage(UUID.randomUUID(), MessageRole.ASSISTANT, "stream-reply", Instant.now()));
            return new AgentConversationTurn(updated, "stream-reply", List.of());
        }
    }

    private static final class InterruptibleStreamingAgent implements ConversationAgent {
        private List<String> pendingMessages = List.of();

        @Override
        public AgentConversationTurn continueConversation(SessionContext sessionContext, String prompt, String model) {
            throw new AssertionError("streaming path expected");
        }

        @Override
        public AgentConversationTurn continueConversationStreaming(
                SessionContext sessionContext,
                String prompt,
                String model,
                Consumer<String> textDeltaConsumer,
                TurnInterruptionSource turnInterruptionSource
        ) {
            pendingMessages = turnInterruptionSource.drainPendingMessages().stream()
                    .map(ConversationMessage::content)
                    .toList();
            SessionContext updated = sessionContext
                    .append(ConversationMessage.user(prompt))
                    .append(new ConversationMessage(UUID.randomUUID(), MessageRole.ASSISTANT, "interrupt-aware-reply", Instant.now()));
            return new AgentConversationTurn(updated, "interrupt-aware-reply", List.of());
        }
    }
}

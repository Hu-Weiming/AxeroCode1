package com.axercode.storage.sqlite.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axercode.core.session.ConversationMessage;
import com.axercode.core.session.MessageRole;
import com.axercode.core.session.SessionContext;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteSessionRepositoryTest {

    @Test
    void saveCurrentSessionLoadsTheSameSessionAndMessageOrder(@TempDir Path tempDir) {
        SqliteSessionRepository repository = new SqliteSessionRepository(tempDir.resolve("axercode.db"));
        SessionContext session = SessionContext.start()
                .append(userMessage("hello", "2026-04-18T15:40:00Z"))
                .append(assistantMessage("hi there", "2026-04-18T15:40:01Z"))
                .append(userMessage("summarize", "2026-04-18T15:40:02Z"));

        repository.saveCurrentSession(session);

        SessionContext loaded = repository.loadCurrentSession().orElseThrow();

        assertEquals(session.sessionId(), loaded.sessionId());
        assertEquals(3, loaded.messages().size());
        assertEquals("hello", loaded.messages().get(0).content());
        assertEquals("hi there", loaded.messages().get(1).content());
        assertEquals("summarize", loaded.messages().get(2).content());
    }

    @Test
    void saveCurrentSessionSwitchesActiveSessionWithoutLosingOlderSession(@TempDir Path tempDir) {
        SqliteSessionRepository repository = new SqliteSessionRepository(tempDir.resolve("axercode.db"));
        SessionContext firstSession = SessionContext.start()
                .append(userMessage("first", "2026-04-18T15:41:00Z"));
        SessionContext secondSession = SessionContext.start()
                .append(userMessage("second", "2026-04-18T15:41:10Z"))
                .append(assistantMessage("second-reply", "2026-04-18T15:41:11Z"));

        repository.saveCurrentSession(firstSession);
        repository.saveCurrentSession(secondSession);

        SessionContext current = repository.loadCurrentSession().orElseThrow();
        SessionContext restoredFirst = repository.loadSession(firstSession.sessionId()).orElseThrow();

        assertEquals(secondSession.sessionId(), current.sessionId());
        assertEquals("second", current.messages().get(0).content());
        assertEquals("first", restoredFirst.messages().getFirst().content());
        assertTrue(repository.loadSession(secondSession.sessionId()).isPresent());
    }

    @Test
    void listSessionsReturnsNewestFirstWithSidebarSummaryFields(@TempDir Path tempDir) throws Exception {
        SqliteSessionRepository repository = new SqliteSessionRepository(tempDir.resolve("axercode.db"));
        SessionContext firstSession = SessionContext.start()
                .append(userMessage("Plan the layout refresh", "2026-04-18T15:42:00Z"))
                .append(assistantMessage("Captured the baseline UI.", "2026-04-18T15:42:01Z"));
        SessionContext secondSession = SessionContext.start()
                .append(userMessage("Build the sidebar", "2026-04-18T15:43:00Z"))
                .append(assistantMessage("Added server-backed history list.", "2026-04-18T15:43:01Z"));

        repository.saveSession(firstSession);
        Thread.sleep(25L);
        repository.saveSession(secondSession);

        var sessions = repository.listSessions(10);

        assertEquals(2, sessions.size());
        assertEquals(secondSession.sessionId().toString(), sessions.get(0).sessionId());
        assertEquals("Build the sidebar", sessions.get(0).title());
        assertEquals("Added server-backed history list.", sessions.get(0).preview());
        assertEquals(2, sessions.get(0).messageCount());
        assertEquals(firstSession.sessionId().toString(), sessions.get(1).sessionId());
    }

    @Test
    void saveSessionPreservesExistingAssistantModelsAndAssignsNewModelToNewAssistantMessages(@TempDir Path tempDir) {
        SqliteSessionRepository repository = new SqliteSessionRepository(tempDir.resolve("axercode.db"));
        SessionContext session = SessionContext.start();

        ConversationMessage firstUser = userMessage("Plan with Claude", "2026-04-19T10:00:00Z");
        ConversationMessage firstAssistant = assistantMessage("Claude answer", "2026-04-19T10:00:01Z");
        session = session.append(firstUser).append(firstAssistant);
        repository.saveSession(session, "anthropic", "claude-3-5-sonnet-latest");

        ConversationMessage secondUser = userMessage("Continue with qwen", "2026-04-19T10:01:00Z");
        ConversationMessage secondAssistant = assistantMessage("Qwen answer", "2026-04-19T10:01:01Z");
        session = session.append(secondUser).append(secondAssistant);
        repository.saveSession(session, "ollama", "qwen2.5:14b");

        Map<UUID, String> models = repository.loadSessionMessageModels(session.sessionId());

        assertEquals(Map.of(
                firstAssistant.id(), "claude-3-5-sonnet-latest",
                secondAssistant.id(), "qwen2.5:14b"
        ), models);
    }

    private static ConversationMessage userMessage(String content, String instant) {
        return new ConversationMessage(
                UUID.randomUUID(),
                MessageRole.USER,
                content,
                Instant.parse(instant)
        );
    }

    private static ConversationMessage assistantMessage(String content, String instant) {
        return new ConversationMessage(
                UUID.randomUUID(),
                MessageRole.ASSISTANT,
                content,
                Instant.parse(instant)
        );
    }
}

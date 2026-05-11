package com.axercode.cli.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.axercode.core.session.ConversationMessage;
import com.axercode.core.session.MessageRole;
import com.axercode.core.session.SessionContext;
import com.axercode.storage.sqlite.session.SqliteSessionRepository;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteBackedSessionStoreTest {

    @Test
    void currentSessionLoadsPersistedCurrentSession(@TempDir Path tempDir) {
        SqliteSessionRepository repository = new SqliteSessionRepository(tempDir.resolve("axercode.db"));
        SessionContext persistedSession = SessionContext.start()
                .append(message(MessageRole.USER, "hello", "2026-04-18T15:50:00Z"));
        repository.saveCurrentSession(persistedSession);

        SqliteBackedSessionStore sessionStore = new SqliteBackedSessionStore(repository);

        assertEquals(persistedSession.sessionId(), sessionStore.currentSession().sessionId());
        assertEquals("hello", sessionStore.currentSession().messages().getFirst().content());
    }

    @Test
    void replaceAndResetPersistSessionChanges(@TempDir Path tempDir) {
        SqliteSessionRepository repository = new SqliteSessionRepository(tempDir.resolve("axercode.db"));
        SqliteBackedSessionStore sessionStore = new SqliteBackedSessionStore(repository);

        SessionContext updatedSession = sessionStore.currentSession()
                .append(message(MessageRole.USER, "persist me", "2026-04-18T15:51:00Z"));
        sessionStore.replace(updatedSession);

        SqliteBackedSessionStore reloadedStore = new SqliteBackedSessionStore(repository);
        assertEquals("persist me", reloadedStore.currentSession().messages().getFirst().content());

        SessionContext resetSession = reloadedStore.reset();
        SqliteBackedSessionStore resetReloadedStore = new SqliteBackedSessionStore(repository);

        assertEquals(resetSession.sessionId(), resetReloadedStore.currentSession().sessionId());
        assertEquals(0, resetReloadedStore.currentSession().messages().size());
        assertNotEquals(updatedSession.sessionId(), resetReloadedStore.currentSession().sessionId());
    }

    private static ConversationMessage message(MessageRole role, String content, String instant) {
        return new ConversationMessage(
                UUID.randomUUID(),
                role,
                content,
                Instant.parse(instant)
        );
    }
}

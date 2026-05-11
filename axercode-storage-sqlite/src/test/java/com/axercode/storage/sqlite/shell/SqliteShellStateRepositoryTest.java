package com.axercode.storage.sqlite.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axercode.core.session.ConversationMessage;
import com.axercode.core.session.MessageRole;
import com.axercode.core.session.SessionContext;
import com.axercode.storage.sqlite.session.SqliteSessionRepository;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteShellStateRepositoryTest {

    @Test
    void saveAndReloadFocusPath(@TempDir Path tempDir) {
        Path databaseFile = tempDir.resolve("axercode.db");
        SqliteShellStateRepository repository = new SqliteShellStateRepository(
                new SqliteSessionRepository(databaseFile)
        );

        repository.saveFocusPath(tempDir);

        SqliteShellStateRepository reloadedRepository = new SqliteShellStateRepository(
                new SqliteSessionRepository(databaseFile)
        );

        assertEquals(tempDir.toAbsolutePath(), reloadedRepository.loadFocusPath().orElseThrow());
    }

    @Test
    void saveAndReloadNamedCheckpoint(@TempDir Path tempDir) {
        Path databaseFile = tempDir.resolve("axercode.db");
        SqliteShellStateRepository repository = new SqliteShellStateRepository(
                new SqliteSessionRepository(databaseFile)
        );
        SessionContext checkpointSession = SessionContext.start()
                .append(message(MessageRole.USER, "hello", "2026-04-18T16:20:00Z"));

        repository.saveCheckpoint("alpha", checkpointSession);

        SqliteShellStateRepository reloadedRepository = new SqliteShellStateRepository(
                new SqliteSessionRepository(databaseFile)
        );
        SessionContext restored = reloadedRepository.loadCheckpoint("alpha").orElseThrow();

        assertNotEquals(checkpointSession.sessionId(), restored.sessionId());
        assertEquals("hello", restored.messages().getFirst().content());
        assertTrue(reloadedRepository.listCheckpointNames().contains("alpha"));
    }

    @Test
    void savedCheckpointRemainsFrozenAfterLiveSessionMutates(@TempDir Path tempDir) {
        Path databaseFile = tempDir.resolve("axercode.db");
        SqliteSessionRepository sessionRepository = new SqliteSessionRepository(databaseFile);
        SqliteShellStateRepository repository = new SqliteShellStateRepository(sessionRepository);
        SessionContext liveSession = SessionContext.start()
                .append(message(MessageRole.USER, "hello", "2026-04-18T16:21:00Z"));

        repository.saveCheckpoint("alpha", liveSession);
        sessionRepository.saveCurrentSession(liveSession.append(message(MessageRole.ASSISTANT, "mutated", "2026-04-18T16:21:05Z")));

        SessionContext restored = repository.loadCheckpoint("alpha").orElseThrow();

        assertEquals(1, restored.messages().size());
        assertEquals("hello", restored.messages().getFirst().content());
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

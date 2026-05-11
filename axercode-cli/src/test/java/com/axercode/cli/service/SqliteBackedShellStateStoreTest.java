package com.axercode.cli.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axercode.core.session.ConversationMessage;
import com.axercode.core.session.MessageRole;
import com.axercode.core.session.SessionContext;
import com.axercode.storage.sqlite.session.SqliteSessionRepository;
import com.axercode.storage.sqlite.shell.SqliteShellStateRepository;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteBackedShellStateStoreTest {

    @Test
    void focusAndCheckpointReloadAcrossStoreInstances(@TempDir Path tempDir) {
        Path databaseFile = tempDir.resolve("axercode.db");
        SqliteBackedShellStateStore store = new SqliteBackedShellStateStore(
                new SqliteShellStateRepository(new SqliteSessionRepository(databaseFile))
        );
        SessionContext checkpointSession = SessionContext.start()
                .append(message(MessageRole.USER, "persist me", "2026-04-18T16:25:00Z"));

        store.setFocusPath(tempDir);
        store.saveCheckpoint("alpha", checkpointSession);
        store.setPlanModeEnabled(true);
        store.saveBranch("feature-a", checkpointSession);

        SqliteBackedShellStateStore reloadedStore = new SqliteBackedShellStateStore(
                new SqliteShellStateRepository(new SqliteSessionRepository(databaseFile))
        );

        assertEquals(tempDir.toAbsolutePath(), reloadedStore.focusPath().orElseThrow());
        assertNotEquals(checkpointSession.sessionId(), reloadedStore.loadCheckpoint("alpha").orElseThrow().sessionId());
        assertTrue(reloadedStore.checkpointNames().contains("alpha"));
        assertEquals("alpha", reloadedStore.activeCheckpointName().orElseThrow());
        assertTrue(reloadedStore.planModeEnabled());
        assertEquals("feature-a", reloadedStore.activeBranchName().orElseThrow());
        assertEquals(checkpointSession.sessionId(), reloadedStore.loadBranch("feature-a").orElseThrow().sessionId());
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

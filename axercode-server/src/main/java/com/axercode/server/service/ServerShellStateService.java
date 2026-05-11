package com.axercode.server.service;

import com.axercode.core.session.ConversationMessage;
import com.axercode.core.session.MessageRole;
import com.axercode.storage.sqlite.shell.SqliteShellStateRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Server-side view of the shared shell-state metadata stored in SQLite.
 */
public class ServerShellStateService {

    private final SqliteShellStateRepository repository;

    public ServerShellStateService(SqliteShellStateRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    public ServerShellState loadState() {
        return new ServerShellState(
                repository.loadPlanModeEnabled(),
                repository.loadFocusPath().map(path -> path.toAbsolutePath().toString()).orElse(null),
                repository.loadActiveCheckpointName().orElse(null),
                repository.loadActiveBranchName().orElse(null),
                repository.listCheckpointNames().size()
        );
    }

    public void setPlanModeEnabled(boolean enabled) {
        repository.savePlanModeEnabled(enabled);
    }

    public List<ConversationMessage> contextMessages() {
        List<ConversationMessage> messages = new ArrayList<>();

        repository.loadFocusPath().ifPresent(path -> messages.add(new ConversationMessage(
                UUID.randomUUID(),
                MessageRole.SYSTEM,
                "Current focus path: " + path.toAbsolutePath()
                        + ". Prefer this path when resolving relative project references for this turn.",
                Instant.now()
        )));

        repository.loadActiveCheckpointName().ifPresent(checkpointName -> messages.add(new ConversationMessage(
                UUID.randomUUID(),
                MessageRole.SYSTEM,
                "Active checkpoint: " + checkpointName
                        + ". Treat the current session as working from this named rollback point when the user refers to restoring, comparing, or continuing from a checkpoint.",
                Instant.now()
        )));

        repository.loadActiveBranchName().ifPresent(branchName -> messages.add(new ConversationMessage(
                UUID.randomUUID(),
                MessageRole.SYSTEM,
                "Current session branch: " + branchName
                        + ". Treat this conversation as working on that named branch unless the user explicitly switches branches.",
                Instant.now()
        )));

        if (repository.loadPlanModeEnabled()) {
            messages.add(new ConversationMessage(
                    UUID.randomUUID(),
                    MessageRole.SYSTEM,
                    "Plan mode is enabled. Respond with a concise phased or numbered plan, emphasize analysis and next steps, and do not imply the work has already been executed.",
                    Instant.now()
            ));
        }

        return List.copyOf(messages);
    }
}

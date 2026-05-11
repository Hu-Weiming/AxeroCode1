package com.axercode.cli.service;

import com.axercode.core.session.ConversationMessage;
import com.axercode.core.session.MessageRole;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Builds temporary system-context messages from shell workspace state.
 */
public class ShellContextAugmenter {

    private final ShellStateStore shellStateStore;

    public ShellContextAugmenter(ShellStateStore shellStateStore) {
        this.shellStateStore = shellStateStore;
    }

    public List<ConversationMessage> systemMessages() {
        List<ConversationMessage> messages = new java.util.ArrayList<>();

        shellStateStore.focusPath().ifPresent(path -> messages.add(new ConversationMessage(
                UUID.randomUUID(),
                MessageRole.SYSTEM,
                "Current focus path: " + path.toAbsolutePath()
                        + ". Prefer this path when resolving relative project references for this turn.",
                Instant.now()
        )));

        shellStateStore.activeCheckpointName().ifPresent(checkpointName -> messages.add(new ConversationMessage(
                UUID.randomUUID(),
                MessageRole.SYSTEM,
                "Active checkpoint: " + checkpointName
                        + ". Treat the current session as working from this named rollback point when the user refers to restoring, comparing, or continuing from a checkpoint.",
                Instant.now()
        )));

        shellStateStore.activeBranchName().ifPresent(branchName -> messages.add(new ConversationMessage(
                UUID.randomUUID(),
                MessageRole.SYSTEM,
                "Current session branch: " + branchName
                        + ". Treat this conversation as working on that named branch unless the user explicitly switches branches.",
                Instant.now()
        )));

        if (shellStateStore.planModeEnabled()) {
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

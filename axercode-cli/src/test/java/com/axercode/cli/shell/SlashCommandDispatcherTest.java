package com.axercode.cli.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axercode.cli.service.InMemoryShellStateStore;
import com.axercode.cli.service.InMemorySessionStore;
import com.axercode.core.session.ConversationMessage;
import com.axercode.core.session.SessionContext;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SlashCommandDispatcherTest {

    @Test
    void handleStatusAndFocusCommands(@TempDir Path tempDir) {
        SlashCommandDispatcher dispatcher = new SlashCommandDispatcher();
        InMemorySessionStore sessionStore = new InMemorySessionStore();
        InMemoryShellStateStore shellStateStore = new InMemoryShellStateStore();

        ShellCommandResult status = dispatcher.handle("/status", sessionStore, shellStateStore, "qwen2.5:7b");

        assertTrue(status.outputLines().stream().anyMatch(line -> line.contains("Model: qwen2.5:7b")));
        assertTrue(status.outputLines().stream().anyMatch(line -> line.contains("Focus: <none>")));
        assertTrue(status.outputLines().stream().anyMatch(line -> line.contains("Active checkpoint: <none>")));
        assertTrue(status.outputLines().stream().anyMatch(line -> line.contains("Active branch: <none>")));
        assertTrue(status.outputLines().stream().anyMatch(line -> line.contains("Plan mode: off")));
        assertTrue(status.outputLines().stream().anyMatch(line -> line.contains("Checkpoints: 0")));

        ShellCommandResult setFocus = dispatcher.handle("/focus " + tempDir, sessionStore, shellStateStore, "qwen2.5:7b");
        assertTrue(setFocus.outputLines().stream().anyMatch(line -> line.contains(tempDir.toAbsolutePath().toString())));

        ShellCommandResult showFocus = dispatcher.handle("/focus", sessionStore, shellStateStore, "qwen2.5:7b");
        assertEquals("Focus: " + tempDir.toAbsolutePath(), showFocus.outputLines().getFirst());

        ShellCommandResult clearFocus = dispatcher.handle("/focus clear", sessionStore, shellStateStore, "qwen2.5:7b");
        assertEquals("[AxerCode] Cleared focus path.", clearFocus.outputLines().getFirst());
    }

    @Test
    void handleCheckpointListAndRestoreCommands() {
        SlashCommandDispatcher dispatcher = new SlashCommandDispatcher();
        InMemorySessionStore sessionStore = new InMemorySessionStore();
        InMemoryShellStateStore shellStateStore = new InMemoryShellStateStore();

        SessionContext original = sessionStore.replace(SessionContext.start().append(ConversationMessage.user("hello")));

        ShellCommandResult checkpoint = dispatcher.handle("/checkpoint alpha", sessionStore, shellStateStore, "qwen2.5:7b");
        assertEquals("[AxerCode] Saved checkpoint 'alpha' (1 messages).", checkpoint.outputLines().getFirst());
        assertEquals("alpha", shellStateStore.activeCheckpointName().orElseThrow());

        sessionStore.reset();
        ShellCommandResult restore = dispatcher.handle("/restore alpha", sessionStore, shellStateStore, "qwen2.5:7b");

        assertEquals("[AxerCode] Restored checkpoint 'alpha' (1 messages).", restore.outputLines().getFirst());
        assertNotEquals(original.sessionId(), sessionStore.currentSession().sessionId());
        assertEquals(1, sessionStore.currentSession().messages().size());
        assertEquals("hello", sessionStore.currentSession().messages().getFirst().content());
        assertEquals("alpha", shellStateStore.activeCheckpointName().orElseThrow());

        ShellCommandResult checkpoints = dispatcher.handle("/checkpoints", sessionStore, shellStateStore, "qwen2.5:7b");
        assertTrue(checkpoints.outputLines().stream().anyMatch(line -> line.contains("alpha")));
    }

    @Test
    void handleDiffAgainstActiveCheckpoint() {
        SlashCommandDispatcher dispatcher = new SlashCommandDispatcher();
        InMemorySessionStore sessionStore = new InMemorySessionStore();
        InMemoryShellStateStore shellStateStore = new InMemoryShellStateStore();

        shellStateStore.saveCheckpoint("alpha", SessionContext.start().append(ConversationMessage.user("hello")));
        sessionStore.replace(
                SessionContext.start()
                        .append(ConversationMessage.user("hello"))
                        .append(ConversationMessage.user("follow-up"))
        );

        ShellCommandResult diff = dispatcher.handle("/diff", sessionStore, shellStateStore, "qwen2.5:7b");

        assertEquals("Diff against active checkpoint 'alpha':", diff.outputLines().getFirst());
        assertTrue(diff.outputLines().stream().anyMatch(line -> line.equals("Common prefix messages: 1")));
        assertTrue(diff.outputLines().stream().anyMatch(line -> line.equals("Current-only messages: 1")));
        assertTrue(diff.outputLines().stream().anyMatch(line -> line.equals("+ USER: follow-up")));
    }

    @Test
    void handleDiffAgainstNamedBranch() {
        SlashCommandDispatcher dispatcher = new SlashCommandDispatcher();
        InMemorySessionStore sessionStore = new InMemorySessionStore();
        InMemoryShellStateStore shellStateStore = new InMemoryShellStateStore();

        shellStateStore.saveBranch(
                "feature-a",
                SessionContext.start()
                        .append(ConversationMessage.user("hello"))
                        .append(ConversationMessage.user("branch note"))
        );
        sessionStore.replace(
                SessionContext.start()
                        .append(ConversationMessage.user("hello"))
                        .append(ConversationMessage.user("current note"))
        );

        ShellCommandResult diff = dispatcher.handle("/diff branch feature-a", sessionStore, shellStateStore, "qwen2.5:7b");

        assertEquals("Diff against branch 'feature-a':", diff.outputLines().getFirst());
        assertTrue(diff.outputLines().stream().anyMatch(line -> line.equals("+ USER: current note")));
        assertTrue(diff.outputLines().stream().anyMatch(line -> line.equals("- USER: branch note")));
    }

    @Test
    void handleDiffWithoutReferenceShowsGuidance() {
        SlashCommandDispatcher dispatcher = new SlashCommandDispatcher();
        InMemorySessionStore sessionStore = new InMemorySessionStore();
        InMemoryShellStateStore shellStateStore = new InMemoryShellStateStore();

        ShellCommandResult diff = dispatcher.handle("/diff", sessionStore, shellStateStore, "qwen2.5:7b");

        assertEquals(
                "[AxerCode] No active checkpoint or branch to diff against. Try /checkpoint <name>, /branch <name>, /diff checkpoint <name>, or /diff branch <name>.",
                diff.outputLines().getFirst()
        );
    }

    @Test
    void handleNewClearsActiveCheckpoint() {
        SlashCommandDispatcher dispatcher = new SlashCommandDispatcher();
        InMemorySessionStore sessionStore = new InMemorySessionStore();
        InMemoryShellStateStore shellStateStore = new InMemoryShellStateStore();

        sessionStore.replace(SessionContext.start().append(ConversationMessage.user("hello")));
        dispatcher.handle("/checkpoint alpha", sessionStore, shellStateStore, "qwen2.5:7b");

        ShellCommandResult result = dispatcher.handle("/new", sessionStore, shellStateStore, "qwen2.5:7b");

        assertEquals("[AxerCode] Started a new in-memory session.", result.outputLines().getFirst());
        assertTrue(shellStateStore.activeCheckpointName().isEmpty());
    }

    @Test
    void handlePlanCommands() {
        SlashCommandDispatcher dispatcher = new SlashCommandDispatcher();
        InMemorySessionStore sessionStore = new InMemorySessionStore();
        InMemoryShellStateStore shellStateStore = new InMemoryShellStateStore();

        ShellCommandResult showInitial = dispatcher.handle("/plan", sessionStore, shellStateStore, "qwen2.5:7b");
        assertEquals("Plan mode: off", showInitial.outputLines().getFirst());

        ShellCommandResult enable = dispatcher.handle("/plan on", sessionStore, shellStateStore, "qwen2.5:7b");
        assertEquals("[AxerCode] Plan mode enabled.", enable.outputLines().getFirst());
        assertTrue(shellStateStore.planModeEnabled());

        ShellCommandResult status = dispatcher.handle("/plan status", sessionStore, shellStateStore, "qwen2.5:7b");
        assertEquals("Plan mode: on", status.outputLines().getFirst());

        ShellCommandResult disable = dispatcher.handle("/plan off", sessionStore, shellStateStore, "qwen2.5:7b");
        assertEquals("[AxerCode] Plan mode disabled.", disable.outputLines().getFirst());
        assertTrue(!shellStateStore.planModeEnabled());
    }

    @Test
    void handleBranchCreateSwitchAndListCommands() {
        SlashCommandDispatcher dispatcher = new SlashCommandDispatcher();
        InMemorySessionStore sessionStore = new InMemorySessionStore();
        InMemoryShellStateStore shellStateStore = new InMemoryShellStateStore();
        SessionContext original = sessionStore.replace(SessionContext.start().append(ConversationMessage.user("hello")));

        ShellCommandResult create = dispatcher.handle("/branch feature-a", sessionStore, shellStateStore, "qwen2.5:7b");
        assertEquals("[AxerCode] Created and switched to branch 'feature-a'.", create.outputLines().getFirst());
        assertEquals("feature-a", shellStateStore.activeBranchName().orElseThrow());
        assertNotEquals(original.sessionId(), sessionStore.currentSession().sessionId());
        assertEquals(List.of("hello"), sessionStore.currentSession().messages().stream().map(ConversationMessage::content).toList());

        ShellCommandResult status = dispatcher.handle("/branch", sessionStore, shellStateStore, "qwen2.5:7b");
        assertEquals("Active branch: feature-a", status.outputLines().getFirst());

        ShellCommandResult list = dispatcher.handle("/branch list", sessionStore, shellStateStore, "qwen2.5:7b");
        assertTrue(list.outputLines().stream().anyMatch(line -> line.contains("feature-a")));

        SessionContext branched = sessionStore.currentSession();
        sessionStore.reset();
        ShellCommandResult switchExisting = dispatcher.handle("/branch feature-a", sessionStore, shellStateStore, "qwen2.5:7b");
        assertEquals("[AxerCode] Switched to branch 'feature-a'.", switchExisting.outputLines().getFirst());
        assertEquals(branched.sessionId(), sessionStore.currentSession().sessionId());
    }
}

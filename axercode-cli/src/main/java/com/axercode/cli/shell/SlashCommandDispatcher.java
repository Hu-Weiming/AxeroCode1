package com.axercode.cli.shell;

import com.axercode.cli.service.ShellStateStore;
import com.axercode.cli.service.SessionStore;
import com.axercode.core.session.ConversationMessage;
import com.axercode.core.session.SessionContext;
import com.axercode.core.session.SessionContextBrancher;
import com.axercode.core.session.SessionContextDiffer;
import com.axercode.core.session.SessionDiff;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses and executes slash commands for the interactive shell.
 */
public final class SlashCommandDispatcher {

    public ShellCommandResult handle(
            String input,
            SessionStore sessionStore,
            ShellStateStore shellStateStore,
            String effectiveModel
    ) {
        if (input == null || input.isBlank() || !input.startsWith("/")) {
            throw new IllegalArgumentException("input must be a slash command");
        }

        String withoutSlash = input.substring(1).trim();
        String command = withoutSlash;
        String argument = "";
        int spaceIndex = withoutSlash.indexOf(' ');
        if (spaceIndex >= 0) {
            command = withoutSlash.substring(0, spaceIndex).trim();
            argument = withoutSlash.substring(spaceIndex + 1).trim();
        }

        return switch (command) {
            case "help" -> new ShellCommandResult(true, helpLines());
            case "history" -> new ShellCommandResult(true, historyLines(sessionStore.currentSession()));
            case "new" -> {
                sessionStore.reset();
                shellStateStore.clearActiveCheckpointName();
                shellStateStore.clearActiveBranchName();
                yield new ShellCommandResult(true, List.of("[AxerCode] Started a new in-memory session."));
            }
            case "exit" -> new ShellCommandResult(false, List.of("[AxerCode] Leaving interactive shell."));
            case "status" -> new ShellCommandResult(true, statusLines(sessionStore.currentSession(), shellStateStore, effectiveModel));
            case "plan" -> handlePlan(argument, shellStateStore);
            case "focus" -> handleFocus(argument, shellStateStore);
            case "checkpoint" -> handleCheckpoint(argument, sessionStore.currentSession(), shellStateStore);
            case "checkpoints" -> new ShellCommandResult(true, checkpointLines(shellStateStore));
            case "restore" -> handleRestore(argument, sessionStore, shellStateStore);
            case "diff" -> handleDiff(argument, sessionStore.currentSession(), shellStateStore);
            case "branch" -> handleBranch(argument, sessionStore, shellStateStore);
            default -> new ShellCommandResult(true, List.of("[AxerCode] Unknown command: /" + command));
        };
    }

    private List<String> helpLines() {
        return List.of(
                "/help         Show available commands",
                "/history      Show the current conversation",
                "/new          Start a new in-memory session",
                "/status       Show current shell status",
                "/plan         Show plan mode status",
                "/plan on      Enable plan mode",
                "/plan off     Disable plan mode",
                "/focus        Show the current focus path",
                "/focus <path> Set the current focus path",
                "/focus clear  Clear the current focus path",
                "/checkpoint <name> Save a named session checkpoint",
                "/checkpoints  List saved checkpoints",
                "/restore <name> Restore a named checkpoint",
                "/diff         Diff against the active checkpoint or branch",
                "/diff checkpoint <name> Diff against a named checkpoint",
                "/diff branch <name> Diff against a named branch",
                "/branch       Show the active branch",
                "/branch list  List saved branches",
                "/branch <name> Create or switch to a branch",
                "/exit         Exit the interactive shell"
        );
    }

    private List<String> historyLines(SessionContext sessionContext) {
        if (sessionContext.messages().isEmpty()) {
            return List.of("[AxerCode] No messages in the current session.");
        }

        List<String> lines = new ArrayList<>();
        for (ConversationMessage message : sessionContext.messages()) {
            lines.add(message.role() + ": " + message.content());
        }
        return List.copyOf(lines);
    }

    private List<String> statusLines(SessionContext sessionContext, ShellStateStore shellStateStore, String effectiveModel) {
        String focus = shellStateStore.focusPath()
                .map(Path::toString)
                .orElse("<none>");
        return List.of(
                "Session: " + sessionContext.sessionId(),
                "Messages: " + sessionContext.messages().size(),
                "Model: " + effectiveModel,
                "Plan mode: " + (shellStateStore.planModeEnabled() ? "on" : "off"),
                "Focus: " + focus,
                "Active checkpoint: " + shellStateStore.activeCheckpointName().orElse("<none>"),
                "Active branch: " + shellStateStore.activeBranchName().orElse("<none>"),
                "Checkpoints: " + shellStateStore.checkpointCount()
        );
    }

    private ShellCommandResult handlePlan(String argument, ShellStateStore shellStateStore) {
        if (argument.isBlank() || "status".equalsIgnoreCase(argument)) {
            return new ShellCommandResult(true, List.of("Plan mode: " + (shellStateStore.planModeEnabled() ? "on" : "off")));
        }
        if ("on".equalsIgnoreCase(argument)) {
            shellStateStore.setPlanModeEnabled(true);
            return new ShellCommandResult(true, List.of("[AxerCode] Plan mode enabled."));
        }
        if ("off".equalsIgnoreCase(argument)) {
            shellStateStore.setPlanModeEnabled(false);
            return new ShellCommandResult(true, List.of("[AxerCode] Plan mode disabled."));
        }
        return new ShellCommandResult(true, List.of("[AxerCode] Usage: /plan [on|off|status]"));
    }

    private ShellCommandResult handleFocus(String argument, ShellStateStore shellStateStore) {
        if (argument.isBlank()) {
            return new ShellCommandResult(true, List.of(
                    "Focus: " + shellStateStore.focusPath().map(Path::toString).orElse("<none>")
            ));
        }
        if ("clear".equalsIgnoreCase(argument)) {
            shellStateStore.clearFocusPath();
            return new ShellCommandResult(true, List.of("[AxerCode] Cleared focus path."));
        }

        Path focusPath = Path.of(argument).toAbsolutePath();
        if (!Files.exists(focusPath)) {
            return new ShellCommandResult(true, List.of("[AxerCode] Focus path does not exist: " + focusPath));
        }
        shellStateStore.setFocusPath(focusPath);
        return new ShellCommandResult(true, List.of("[AxerCode] Focus set to: " + focusPath));
    }

    private ShellCommandResult handleCheckpoint(String argument, SessionContext sessionContext, ShellStateStore shellStateStore) {
        if (argument.isBlank()) {
            return new ShellCommandResult(true, List.of("[AxerCode] Usage: /checkpoint <name>"));
        }
        shellStateStore.saveCheckpoint(argument, sessionContext);
        return new ShellCommandResult(
                true,
                List.of("[AxerCode] Saved checkpoint '" + argument + "' (" + sessionContext.messages().size() + " messages).")
        );
    }

    private List<String> checkpointLines(ShellStateStore shellStateStore) {
        if (shellStateStore.checkpointCount() == 0) {
            return List.of("[AxerCode] No checkpoints saved in this shell.");
        }

        List<String> lines = new ArrayList<>();
        lines.add("Checkpoints:");
        for (String checkpointName : shellStateStore.checkpointNames()) {
            lines.add("- " + checkpointName);
        }
        return List.copyOf(lines);
    }

    private ShellCommandResult handleRestore(String argument, SessionStore sessionStore, ShellStateStore shellStateStore) {
        if (argument.isBlank()) {
            return new ShellCommandResult(true, List.of("[AxerCode] Usage: /restore <name>"));
        }

        return shellStateStore.loadCheckpoint(argument)
                .map(sessionContext -> {
                    SessionContext restoredWorkingSession = SessionContextBrancher.branch(sessionContext);
                    sessionStore.replace(restoredWorkingSession);
                    shellStateStore.setActiveCheckpointName(argument);
                    return new ShellCommandResult(
                            true,
                            List.of("[AxerCode] Restored checkpoint '" + argument + "' ("
                                    + sessionContext.messages().size() + " messages).")
                    );
                })
                .orElseGet(() -> new ShellCommandResult(
                        true,
                        List.of("[AxerCode] Unknown checkpoint: " + argument)
                ));
    }

    private ShellCommandResult handleDiff(String argument, SessionContext currentSession, ShellStateStore shellStateStore) {
        if (argument.isBlank()) {
            if (shellStateStore.activeCheckpointName().isPresent()) {
                String checkpointName = shellStateStore.activeCheckpointName().orElseThrow();
                return renderNamedCheckpointDiff(checkpointName, currentSession, shellStateStore, "active checkpoint");
            }
            if (shellStateStore.activeBranchName().isPresent()) {
                String branchName = shellStateStore.activeBranchName().orElseThrow();
                return renderNamedBranchDiff(branchName, currentSession, shellStateStore, "active branch");
            }
            return new ShellCommandResult(true, List.of(
                    "[AxerCode] No active checkpoint or branch to diff against. Try /checkpoint <name>, /branch <name>, /diff checkpoint <name>, or /diff branch <name>."
            ));
        }

        if (argument.startsWith("checkpoint ")) {
            String checkpointName = argument.substring("checkpoint ".length()).trim();
            if (checkpointName.isBlank()) {
                return new ShellCommandResult(true, List.of("[AxerCode] Usage: /diff checkpoint <name>"));
            }
            return renderNamedCheckpointDiff(checkpointName, currentSession, shellStateStore, "checkpoint");
        }
        if (argument.startsWith("branch ")) {
            String branchName = argument.substring("branch ".length()).trim();
            if (branchName.isBlank()) {
                return new ShellCommandResult(true, List.of("[AxerCode] Usage: /diff branch <name>"));
            }
            return renderNamedBranchDiff(branchName, currentSession, shellStateStore, "branch");
        }

        return new ShellCommandResult(true, List.of("[AxerCode] Usage: /diff [checkpoint <name>|branch <name>]"));
    }

    private ShellCommandResult handleBranch(String argument, SessionStore sessionStore, ShellStateStore shellStateStore) {
        if (argument.isBlank() || "status".equalsIgnoreCase(argument)) {
            return new ShellCommandResult(true, List.of(
                    "Active branch: " + shellStateStore.activeBranchName().orElse("<none>")
            ));
        }
        if ("list".equalsIgnoreCase(argument)) {
            return new ShellCommandResult(true, branchLines(shellStateStore));
        }

        return shellStateStore.loadBranch(argument)
                .map(existingSession -> {
                    sessionStore.replace(existingSession);
                    shellStateStore.setActiveBranchName(argument);
                    return new ShellCommandResult(true, List.of("[AxerCode] Switched to branch '" + argument + "'."));
                })
                .orElseGet(() -> {
                    SessionContext branchedSession = SessionContextBrancher.branch(sessionStore.currentSession());
                    shellStateStore.saveBranch(argument, branchedSession);
                    sessionStore.replace(branchedSession);
                    return new ShellCommandResult(
                            true,
                            List.of("[AxerCode] Created and switched to branch '" + argument + "'.")
                    );
                });
    }

    private List<String> branchLines(ShellStateStore shellStateStore) {
        if (shellStateStore.branchNames().isEmpty()) {
            return List.of("[AxerCode] No branches saved in this shell.");
        }

        List<String> lines = new ArrayList<>();
        lines.add("Branches:");
        for (String branchName : shellStateStore.branchNames()) {
            lines.add("- " + branchName);
        }
        return List.copyOf(lines);
    }

    private ShellCommandResult renderNamedCheckpointDiff(
            String checkpointName,
            SessionContext currentSession,
            ShellStateStore shellStateStore,
            String labelKind
    ) {
        return shellStateStore.loadCheckpoint(checkpointName)
                .map(referenceSession -> new ShellCommandResult(
                        true,
                        diffLines(
                                "Diff against " + labelKind + " '" + checkpointName + "':",
                                SessionContextDiffer.diff(currentSession, referenceSession)
                        )
                ))
                .orElseGet(() -> new ShellCommandResult(
                        true,
                        List.of("[AxerCode] Unknown checkpoint: " + checkpointName)
                ));
    }

    private ShellCommandResult renderNamedBranchDiff(
            String branchName,
            SessionContext currentSession,
            ShellStateStore shellStateStore,
            String labelKind
    ) {
        return shellStateStore.loadBranch(branchName)
                .map(referenceSession -> new ShellCommandResult(
                        true,
                        diffLines(
                                "Diff against " + labelKind + " '" + branchName + "':",
                                SessionContextDiffer.diff(currentSession, referenceSession)
                        )
                ))
                .orElseGet(() -> new ShellCommandResult(
                        true,
                        List.of("[AxerCode] Unknown branch: " + branchName)
                ));
    }

    private List<String> diffLines(String title, SessionDiff diff) {
        List<String> lines = new ArrayList<>();
        lines.add(title);
        lines.add("Common prefix messages: " + diff.commonPrefixCount());

        if (diff.identical()) {
            lines.add("[AxerCode] No differences.");
            return List.copyOf(lines);
        }

        lines.add("Current-only messages: " + diff.currentOnlyMessages().size());
        for (ConversationMessage message : diff.currentOnlyMessages()) {
            lines.add("+ " + message.role() + ": " + message.content());
        }

        lines.add("Reference-only messages: " + diff.referenceOnlyMessages().size());
        for (ConversationMessage message : diff.referenceOnlyMessages()) {
            lines.add("- " + message.role() + ": " + message.content());
        }
        return List.copyOf(lines);
    }
}

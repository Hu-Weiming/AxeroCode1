package com.axercode.cli.service;

import com.axercode.core.session.SessionContext;
import com.axercode.core.session.SessionContextBrancher;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Keeps shell focus and named checkpoints in memory for tests or non-persistent runs.
 */
public class InMemoryShellStateStore implements ShellStateStore {

    private Path focusPath;
    private String activeCheckpointName;
    private String activeBranchName;
    private boolean planModeEnabled;
    private final Map<String, SessionContext> checkpoints = new LinkedHashMap<>();
    private final Map<String, SessionContext> branches = new LinkedHashMap<>();

    @Override
    public Optional<Path> focusPath() {
        return Optional.ofNullable(focusPath);
    }

    @Override
    public Optional<String> activeCheckpointName() {
        return Optional.ofNullable(activeCheckpointName);
    }

    @Override
    public Optional<String> activeBranchName() {
        return Optional.ofNullable(activeBranchName);
    }

    @Override
    public boolean planModeEnabled() {
        return planModeEnabled;
    }

    @Override
    public void setFocusPath(Path focusPath) {
        this.focusPath = focusPath == null ? null : focusPath.toAbsolutePath();
    }

    @Override
    public void clearFocusPath() {
        this.focusPath = null;
    }

    @Override
    public void setActiveCheckpointName(String checkpointName) {
        this.activeCheckpointName = checkpointName;
    }

    @Override
    public void clearActiveCheckpointName() {
        this.activeCheckpointName = null;
    }

    @Override
    public void setActiveBranchName(String branchName) {
        this.activeBranchName = branchName;
    }

    @Override
    public void clearActiveBranchName() {
        this.activeBranchName = null;
    }

    @Override
    public void setPlanModeEnabled(boolean enabled) {
        this.planModeEnabled = enabled;
    }

    @Override
    public void saveCheckpoint(String name, SessionContext sessionContext) {
        checkpoints.put(name, SessionContextBrancher.branch(sessionContext));
        activeCheckpointName = name;
    }

    @Override
    public Optional<SessionContext> loadCheckpoint(String name) {
        return Optional.ofNullable(checkpoints.get(name));
    }

    @Override
    public void saveBranch(String name, SessionContext sessionContext) {
        branches.put(name, sessionContext);
        activeBranchName = name;
    }

    @Override
    public Optional<SessionContext> loadBranch(String name) {
        return Optional.ofNullable(branches.get(name));
    }

    @Override
    public List<String> checkpointNames() {
        return List.copyOf(checkpoints.keySet());
    }

    @Override
    public List<String> branchNames() {
        return List.copyOf(branches.keySet());
    }

    @Override
    public int checkpointCount() {
        return checkpoints.size();
    }
}

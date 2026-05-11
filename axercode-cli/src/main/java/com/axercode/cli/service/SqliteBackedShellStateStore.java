package com.axercode.cli.service;

import com.axercode.core.session.SessionContext;
import com.axercode.storage.sqlite.shell.SqliteShellStateRepository;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Production shell state store backed by SQLite.
 */
@Component
public class SqliteBackedShellStateStore implements ShellStateStore {

    private final SqliteShellStateRepository repository;

    public SqliteBackedShellStateStore(SqliteShellStateRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Path> focusPath() {
        return repository.loadFocusPath();
    }

    @Override
    public Optional<String> activeCheckpointName() {
        return repository.loadActiveCheckpointName();
    }

    @Override
    public Optional<String> activeBranchName() {
        return repository.loadActiveBranchName();
    }

    @Override
    public boolean planModeEnabled() {
        return repository.loadPlanModeEnabled();
    }

    @Override
    public void setFocusPath(Path focusPath) {
        repository.saveFocusPath(focusPath);
    }

    @Override
    public void clearFocusPath() {
        repository.clearFocusPath();
    }

    @Override
    public void setActiveCheckpointName(String checkpointName) {
        repository.saveActiveCheckpointName(checkpointName);
    }

    @Override
    public void clearActiveCheckpointName() {
        repository.clearActiveCheckpointName();
    }

    @Override
    public void setActiveBranchName(String branchName) {
        repository.saveActiveBranchName(branchName);
    }

    @Override
    public void clearActiveBranchName() {
        repository.clearActiveBranchName();
    }

    @Override
    public void setPlanModeEnabled(boolean enabled) {
        repository.savePlanModeEnabled(enabled);
    }

    @Override
    public void saveCheckpoint(String name, SessionContext sessionContext) {
        repository.saveCheckpoint(name, sessionContext);
        repository.saveActiveCheckpointName(name);
    }

    @Override
    public Optional<SessionContext> loadCheckpoint(String name) {
        return repository.loadCheckpoint(name);
    }

    @Override
    public void saveBranch(String name, SessionContext sessionContext) {
        repository.saveBranch(name, sessionContext);
        repository.saveActiveBranchName(name);
    }

    @Override
    public Optional<SessionContext> loadBranch(String name) {
        return repository.loadBranch(name);
    }

    @Override
    public List<String> checkpointNames() {
        return repository.listCheckpointNames();
    }

    @Override
    public List<String> branchNames() {
        return repository.listBranchNames();
    }

    @Override
    public int checkpointCount() {
        return checkpointNames().size();
    }
}

package com.axercode.cli.service;

import com.axercode.core.session.SessionContext;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Abstraction for persisted or in-memory interactive shell workspace state.
 */
public interface ShellStateStore {

    Optional<Path> focusPath();

    Optional<String> activeCheckpointName();

    Optional<String> activeBranchName();

    boolean planModeEnabled();

    void setFocusPath(Path focusPath);

    void clearFocusPath();

    void setActiveCheckpointName(String checkpointName);

    void clearActiveCheckpointName();

    void setActiveBranchName(String branchName);

    void clearActiveBranchName();

    void setPlanModeEnabled(boolean enabled);

    void saveCheckpoint(String name, SessionContext sessionContext);

    Optional<SessionContext> loadCheckpoint(String name);

    void saveBranch(String name, SessionContext sessionContext);

    Optional<SessionContext> loadBranch(String name);

    List<String> checkpointNames();

    List<String> branchNames();

    int checkpointCount();
}

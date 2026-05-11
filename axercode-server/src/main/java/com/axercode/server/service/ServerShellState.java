package com.axercode.server.service;

/**
 * Snapshot of shared shell-state metadata exposed to the Web client.
 */
public record ServerShellState(
        boolean planModeEnabled,
        String focusPath,
        String activeCheckpointName,
        String activeBranchName,
        int checkpointCount
) {
}

package com.axercode.server.api;

import com.axercode.server.service.ServerShellState;

public record ShellStateResponse(
        boolean planModeEnabled,
        String focusPath,
        String activeCheckpointName,
        String activeBranchName,
        int checkpointCount
) {

    public static ShellStateResponse from(ServerShellState state) {
        return new ShellStateResponse(
                state.planModeEnabled(),
                state.focusPath(),
                state.activeCheckpointName(),
                state.activeBranchName(),
                state.checkpointCount()
        );
    }
}

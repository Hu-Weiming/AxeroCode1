package com.axercode.cli.shell;

import java.util.List;
import java.util.Objects;

/**
 * Output and continuation decision for one slash command.
 */
public record ShellCommandResult(boolean continueShell, List<String> outputLines) {

    public ShellCommandResult {
        Objects.requireNonNull(outputLines, "outputLines must not be null");
        outputLines = List.copyOf(outputLines);
    }
}

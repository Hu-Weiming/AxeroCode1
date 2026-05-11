# Step 14 - Advanced Slash Commands

## Step Goal

Add a dedicated slash-command layer to the interactive CLI and ship the first advanced shell commands for status, focus, and checkpoint workflows.

## What Was Done

1. Extracted slash-command behavior out of the large shell `switch`.
2. Added `ShellRuntimeState` for interactive-only focus path and named checkpoints.
3. Added `SlashCommandDispatcher` with `/status`, `/focus`, `/checkpoint`, `/checkpoints`, and `/restore`.
4. Kept the existing `/help`, `/history`, `/new`, and `/exit` behavior inside the same dispatcher boundary.
5. Updated the JLine completer and help output to reflect the larger command surface.
6. Fixed a real runtime bug in the default shell history path after running the packaged interactive CLI.

## Files Added or Changed

### Production Code

- `axercode-cli/src/main/java/com/axercode/cli/shell/ShellRuntimeState.java`
- `axercode-cli/src/main/java/com/axercode/cli/shell/ShellCommandResult.java`
- `axercode-cli/src/main/java/com/axercode/cli/shell/SlashCommandDispatcher.java`
- `axercode-cli/src/main/java/com/axercode/cli/service/InteractiveShellService.java`
- `axercode-cli/src/main/java/com/axercode/cli/service/CliChatService.java`
- `axercode-cli/src/main/java/com/axercode/cli/config/AxerCodeShellProperties.java`
- `axercode-cli/src/main/resources/application.yml`

### Tests

- `axercode-cli/src/test/java/com/axercode/cli/shell/SlashCommandDispatcherTest.java`
- `axercode-cli/src/test/java/com/axercode/cli/service/InteractiveShellServiceTest.java`

### Documentation

- `docs/superpowers/specs/2026-04-18-axercode-advanced-slash-commands-design.md`
- `docs/superpowers/plans/2026-04-18-axercode-step-14-advanced-slash-commands.md`
- `docs/steps/step-14-advanced-slash-commands.md`

## How It Was Implemented

## 1. Dedicated Slash Command Boundary

Instead of continuing to grow `InteractiveShellService` with more `switch` branches, this step introduced:

- `ShellRuntimeState`
- `ShellCommandResult`
- `SlashCommandDispatcher`

This separates:

- shell input/output flow
- slash-command parsing and behavior
- interactive-only metadata

That boundary is important because later commands such as `/branch` and `/diff` should not be embedded directly in the shell loop.

## 2. Per-Run Interactive State

`ShellRuntimeState` is created fresh for each interactive shell run.

It currently stores:

- optional focus path
- named checkpoints mapped to `SessionContext` snapshots

This state is intentionally transient in Step 14.

That means checkpoints are useful during a shell session, but they are not yet persisted across process restarts.

## 3. First Advanced Commands

The new commands behave like this:

- `/status` shows session id, message count, model, focus path, and checkpoint count
- `/focus` shows the current focus path
- `/focus <path>` sets a focus path if the path exists
- `/focus clear` clears the current focus path
- `/checkpoint <name>` saves the current session snapshot under a name
- `/checkpoints` lists saved checkpoint names
- `/restore <name>` restores the current session from a saved checkpoint

Existing commands still work:

- `/help`
- `/history`
- `/new`
- `/exit`

## 4. Shell Integration

`InteractiveShellService` now delegates any slash-prefixed input to `SlashCommandDispatcher`.

Normal prompts still go through:

1. `CliChatService`
2. agent execution
3. tool feedback rendering
4. final assistant reply

So this step improves the shell command system without changing the agent loop itself.

## 5. Model Resolution Cleanup

`CliChatService` now exposes `resolveModel(...)`.

That lets `/status` show the effective model without duplicating model-selection logic inside the shell layer.

## Runtime Bug and Root Cause

During fresh runtime verification, the packaged interactive CLI failed with:

`FileAlreadyExistsException: C:\Users\胡炜铭\.axercode\history`

The root cause was:

- the configured default history path used `${user.home}/.axercode/history/cli.history`
- but on this machine, `${user.home}/.axercode/history` already existed as a file, not a directory

That made directory creation fail before the shell could start.

The fix was intentionally minimal:

- change the default history file path to `${user.home}/.axercode/cli.history`

This avoids the conflicting path while keeping the history file under the same top-level AxerCode directory.

## Why This Design

The main design choice here was to add structure without over-building a full command framework.

This step is intentionally a middle layer:

- richer than a hardcoded `switch`
- much lighter than embedding Picocli inside the REPL

That keeps Step 14 focused and gives later steps a clean command extension point.

## Important Limitation

This step adds rollback-style checkpoints, but they are currently shell-session-only.

If the process exits, those named checkpoints are gone.

The current persisted SQLite session still remains, but named checkpoint persistence is a future step.

Also, the new focus path is shell metadata only in Step 14. It is shown in `/status`, but it is not yet injected into provider prompts.

## TDD Evidence

This step followed red-green loops at two levels:

1. `SlashCommandDispatcherTest` failed first because the dispatcher and runtime-state types did not exist
2. `InteractiveShellServiceTest` then failed until the shell delegated slash commands correctly

One integration test input string also had to be corrected because the original text-block concatenation malformed the `/focus` line.

## Verification Commands

The following commands were executed successfully in `D:\AeroCode1`:

```bat
scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test "-Dtest=SlashCommandDispatcherTest,InteractiveShellServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"
scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test
scripts\mvn-jdk21.cmd -q test
scripts\mvn-jdk21.cmd -q package
```

## Runtime Verification

The packaged interactive CLI was run successfully with the pinned JDK 21:

```bat
@'
/status
/focus D:\AeroCode1
hello
/checkpoint alpha
/checkpoints
/restore alpha
/exit
'@ | "C:\Program Files\Java\jdk-21\bin\java.exe" -jar D:\AeroCode1\axercode-cli\target\axercode-cli-0.1.0-SNAPSHOT.jar --interactive
```

Observed output included:

- `Model: qwen2.5:7b`
- `[AxerCode] Focus set to: D:\AeroCode1`
- `[AxerCode] Saved checkpoint 'alpha' (2 messages).`
- `Checkpoints:`
- `- alpha`
- `[AxerCode] Restored checkpoint 'alpha' (2 messages).`

## Why This Step Matters

This is the first point where AxerCode has a real advanced shell-command layer instead of only basic utility commands.

It now has:

- richer shell status introspection
- a first focus concept
- lightweight rollback through checkpoints
- a cleaner path for future advanced commands

## Next Step

Step 15 should build on this shell-command layer and start introducing higher-level agent workflows, likely around plan-oriented command behavior or persisted checkpoint/focus metadata.

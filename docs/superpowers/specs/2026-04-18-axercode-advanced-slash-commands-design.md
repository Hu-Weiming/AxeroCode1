# AxerCode Advanced Slash Commands Design

## Goal

Introduce a dedicated slash-command layer for the interactive CLI and add the first advanced session-management commands on top of it.

## Scope

This step covers:

- extracting slash-command handling out of the large `InteractiveShellService` switch
- adding a reusable command dispatcher boundary
- introducing shell runtime state for advanced interactive-only metadata
- adding `/status`, `/focus`, `/checkpoint`, `/checkpoints`, and `/restore`

This step does **not** cover:

- provider-side plan mode logic
- persisted checkpoints across process restarts
- git branching commands
- Web or desktop command surfaces

## Problem

The interactive shell currently handles slash commands with a hardcoded `switch`.

That is still fine for `/help` and `/exit`, but it becomes a scaling problem as soon as we want richer commands such as:

- current shell status
- focus targets
- rollback-style checkpoints
- future `/branch` or `/diff` commands

If we keep growing the `switch`, the shell control flow will become harder to test and harder to extend.

## Approaches Considered

### 1. Keep extending the existing `switch`

Fastest in the short term, but the shell becomes a dumping ground for parsing, state management, and rendering.

### 2. Add a dedicated slash-command dispatcher with shell runtime state

Keeps parsing and command behavior isolated while letting the shell remain focused on input/output flow.

### 3. Jump directly to a full command framework with Picocli inside the REPL

Interesting long term, but too heavy for this step and not necessary to unlock the first advanced commands.

## Recommended Design

Use approach 2.

Add:

1. a `ShellRuntimeState` object for interactive-only metadata
2. a `ShellCommandResult` result type for command handling
3. a `SlashCommandDispatcher` that parses and executes slash commands
4. small updates to `InteractiveShellService` so it delegates slash commands instead of owning all of them

## Command Behavior

### `/status`

Show:

- current session id
- message count
- effective model
- current focus path or `<none>`
- checkpoint count

### `/focus`

Support:

- `/focus` to show current focus
- `/focus <path>` to set a focus path if it exists
- `/focus clear` to clear the current focus

This focus value is interactive-shell metadata for now. It is not yet injected into provider prompts.

### `/checkpoint <name>`

Save the current `SessionContext` under a human-readable name inside shell runtime state.

This is intentionally interactive-only in this step.

### `/checkpoints`

List the saved checkpoint names in insertion order.

### `/restore <name>`

Replace the current session in `SessionStore` with the saved checkpoint snapshot.

This gives the first rollback-style workflow without needing branches yet.

## State Model

`ShellRuntimeState` should hold:

- optional focus path
- ordered named checkpoints mapping to `SessionContext`

This state should be created per interactive run, not as a singleton Spring bean.

That keeps it safe for tests and avoids accidental cross-run leakage.

## Why This Design

The most important design choice here is separating:

- shell input loop
- slash command parsing
- advanced shell runtime state

This keeps the interactive shell thin and prepares the codebase for later command growth.

## Testing Strategy

Add tests for:

- command dispatcher behavior for `/status`, `/focus`, `/checkpoint`, `/checkpoints`, and `/restore`
- shell integration showing the new commands work through the real interactive path
- help text and JLine completer updates if necessary

## Outcome

After this step, AxerCode will have its first structured advanced command layer in the CLI, plus the first lightweight rollback and status workflows inside the interactive shell.

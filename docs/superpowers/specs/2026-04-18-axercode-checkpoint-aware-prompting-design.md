# AxerCode Checkpoint-Aware Prompting Design

## Goal

Extend the existing focus-aware prompt shaping so the CLI can also inject the currently active checkpoint as temporary system context.

## Scope

This step adds:

- active-checkpoint state in the shell store
- persistent storage for that active checkpoint
- prompt injection for active checkpoint context
- slash-command behavior that keeps active checkpoint state in sync

This step does not add diffing, branching, or plan mode.

## Recommended Approach

Treat the active checkpoint exactly like focus-aware prompting:

- shell state owns the durable metadata
- `ShellContextAugmenter` turns that metadata into temporary `SYSTEM` messages
- `CliChatService` prepends and strips those messages per turn

Why this approach:

- it matches the existing focus design
- it keeps checkpoint metadata out of stored conversation history
- it gives the model useful rollback context without mutating the actual session transcript

## Behavior

### Active checkpoint state

The shell store will expose an optional active checkpoint name.

Rules:

- `/checkpoint <name>` saves the current session and marks `<name>` as active
- `/restore <name>` restores that checkpoint and marks `<name>` as active
- `/new` clears the active checkpoint
- shell restarts preserve the active checkpoint in SQLite

### Prompt shaping

`ShellContextAugmenter` will now emit up to two temporary system messages:

1. focus path guidance
2. active checkpoint guidance

The checkpoint guidance tells the model that the current session is working from a named rollback point and that checkpoint-aware user references should prefer that baseline.

### Status output

`/status` should show the active checkpoint name or `<none>`.

## Files Expected To Change

- `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\ShellStateStore.java`
- `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\InMemoryShellStateStore.java`
- `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\SqliteBackedShellStateStore.java`
- `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\ShellContextAugmenter.java`
- `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\shell\SlashCommandDispatcher.java`
- `D:\AeroCode1\axercode-storage-sqlite\src\main\java\com\axercode\storage\sqlite\shell\SqliteShellStateRepository.java`
- `D:\AeroCode1\axercode-cli\src\test\java\com\axercode\cli\shell\SlashCommandDispatcherTest.java`
- `D:\AeroCode1\axercode-cli\src\test\java\com\axercode\cli\service\CliChatServiceTest.java`
- `D:\AeroCode1\axercode-cli\src\test\java\com\axercode\cli\service\SqliteBackedShellStateStoreTest.java`

## Verification Strategy

- shell-state tests for active checkpoint persistence
- slash-command tests for activation, clearing, and status rendering
- CLI service tests proving temporary checkpoint context is injected but not persisted
- full Maven test and package verification on JDK 21

# AxerCode Plan Mode Design

## Goal

Add a first-pass Plan Mode to the CLI so the shell can explicitly switch the model into planning-oriented responses.

## Scope

This step adds:

- shell-level plan mode state
- slash commands to control plan mode
- persistent storage for that mode
- temporary prompt shaping when plan mode is enabled

This step does not add:

- automatic plan execution
- plan approval workflows
- structured plan objects in SQLite

## Recommended Approach

Treat plan mode as shell metadata, just like focus and active checkpoint context.

That means:

- shell state stores whether plan mode is enabled
- slash commands change that state
- `ShellContextAugmenter` turns it into a temporary `SYSTEM` instruction
- the message is not written to session history

Why this approach:

- it fits the existing CLI architecture
- it is easy to persist and restore
- it keeps the transcript clean while still changing model behavior

## Behavior

### Slash commands

Add `/plan` command support:

- `/plan` or `/plan status` shows current state
- `/plan on` enables plan mode
- `/plan off` disables plan mode

`/status` should also show `Plan mode: on` or `Plan mode: off`.

### Persistence

Plan mode should survive CLI restarts through SQLite-backed shell state.

It is a shell-wide mode, not a single-session artifact, so `/new` should not clear it.

### Prompt shaping

When plan mode is enabled, `ShellContextAugmenter` should add a temporary `SYSTEM` instruction telling the model to stay in planning mode:

- prefer phased or numbered plans
- avoid pretending work is already completed
- present next steps before execution claims

This temporary guidance should sit alongside focus and checkpoint context and remain request-only.

## Files Expected To Change

- `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\ShellStateStore.java`
- `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\InMemoryShellStateStore.java`
- `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\SqliteBackedShellStateStore.java`
- `D:\AeroCode1\axercode-storage-sqlite\src\main\java\com\axercode\storage\sqlite\shell\SqliteShellStateRepository.java`
- `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\shell\SlashCommandDispatcher.java`
- `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\ShellContextAugmenter.java`
- `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\InteractiveShellService.java`
- `D:\AeroCode1\axercode-cli\src\test\java\com\axercode\cli\service\SqliteBackedShellStateStoreTest.java`
- `D:\AeroCode1\axercode-cli\src\test\java\com\axercode\cli\shell\SlashCommandDispatcherTest.java`
- `D:\AeroCode1\axercode-cli\src\test\java\com\axercode\cli\service\CliChatServiceTest.java`

## Verification Strategy

- shell-state tests for plan-mode persistence
- slash-command tests for plan-mode control and status rendering
- CLI service tests proving plan-mode prompt guidance is injected but not persisted
- full Maven test and package verification on JDK 21

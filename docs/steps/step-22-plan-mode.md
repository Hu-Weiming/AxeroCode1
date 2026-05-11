# Step 22 - Plan Mode

## This Step Added

Step 22 adds the first usable Plan Mode to AxerCode.

The main result is:

- shell-level plan mode state
- `/plan` slash commands
- persistent plan mode across CLI restarts
- temporary plan-oriented prompt shaping

## Why This Design

The key design choice in this step was to make plan mode shell metadata, not conversation history.

That means:

- the shell decides whether planning mode is active
- the model sees a temporary planning instruction
- `/history`, stored sessions, and checkpoints stay clean

This matches the same architecture already used for focus and active checkpoint context.

## How It Works

### Shell state

`ShellStateStore` now exposes whether plan mode is enabled.

Both:

- `InMemoryShellStateStore`
- `SqliteBackedShellStateStore`

support that flag.

### SQLite persistence

`SqliteShellStateRepository` now stores plan mode in a singleton table.

That makes plan mode survive process restarts, which is useful because it is a shell preference rather than a single-turn detail.

### Slash commands

The shell now supports:

- `/plan`
- `/plan status`
- `/plan on`
- `/plan off`

`/status` also shows `Plan mode: on` or `Plan mode: off`.

### Prompt shaping

When plan mode is enabled, `ShellContextAugmenter` adds a temporary `SYSTEM` instruction:

- respond with a concise phased or numbered plan
- emphasize analysis and next steps
- avoid implying the work has already been executed

This message is request-only context. It is stripped back out before the updated session is returned.

## Verification Run

I verified this step with:

- `scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test "-Dtest=SqliteBackedShellStateStoreTest,SlashCommandDispatcherTest,CliChatServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test`
- `scripts\mvn-jdk21.cmd -q test`
- `scripts\mvn-jdk21.cmd -q package`
- packaged interactive CLI run that enabled `/plan on`, exited, restarted with the same SQLite file, and checked `/status`

The packaged CLI showed:

- `Plan mode: on`

after restart, confirming persistence worked.

## What The Tests Proved

The new tests cover three specific Plan Mode behaviors:

1. the plan-mode flag persists and reloads through SQLite-backed shell state
2. `/plan` commands and `/status` reflect the correct state
3. the plan-mode system message is prepended to provider-facing context but not persisted in session history

## What This Unlocks Next

With Step 22 in place, AxerCode now has the first shell-controlled workflow mode:

- focus-aware prompting
- checkpoint-aware prompting
- plan-aware prompting

That gives the next steps a much stronger base for diff, branch, and richer planning workflows.

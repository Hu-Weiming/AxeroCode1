# Step 20 - Checkpoint-Aware Prompting

## This Step Added

Step 20 makes the active checkpoint part of temporary model context.

The main result is:

- shell state now tracks an active checkpoint name
- SQLite persists that active checkpoint across restarts
- slash commands keep active checkpoint state in sync
- prompt shaping now includes checkpoint-aware temporary system guidance

## Why This Design

The key design choice in this step was to treat active checkpoint metadata the same way focus is treated:

- durable shell state
- temporary prompt context
- no permanent mutation of conversation history

That keeps checkpoint semantics useful to the model without polluting `/history`, saved sessions, or replay logic.

## How It Works

### Shell state

`ShellStateStore` now exposes:

- `activeCheckpointName()`
- `setActiveCheckpointName(...)`
- `clearActiveCheckpointName()`

`InMemoryShellStateStore` and `SqliteBackedShellStateStore` both implement that behavior.

### SQLite persistence

`SqliteShellStateRepository` now stores the active checkpoint in a dedicated singleton table.

That means the current checkpoint context survives process restarts in the same way focus survives restarts.

### Slash commands

The command layer now keeps active checkpoint state aligned with the shell workflow:

- `/checkpoint <name>` saves the session and marks the name active
- `/restore <name>` restores the session and marks the name active
- `/new` clears the active checkpoint
- `/status` shows `Active checkpoint: <name>` or `<none>`

### Prompt shaping

`ShellContextAugmenter` now emits up to two temporary `SYSTEM` messages:

1. current focus path
2. active checkpoint guidance

The checkpoint message tells the model that the current session is working from a named rollback point and that checkpoint-oriented user references should prefer that baseline.

Just like the focus message, this temporary checkpoint message is stripped back out before the updated session is returned to the CLI.

## Verification Run

I verified this step with:

- `scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test "-Dtest=SqliteBackedShellStateStoreTest,SlashCommandDispatcherTest,CliChatServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test`
- `scripts\mvn-jdk21.cmd -q test`
- `scripts\mvn-jdk21.cmd -q package`
- interactive packaged CLI run on JDK 21 with checkpoint save, restore, and a prompt

The interactive packaged CLI returned:

- `[AxerCode] Saved checkpoint 'alpha' (0 messages).`
- `[AxerCode] Restored checkpoint 'alpha' (0 messages).`
- `CHECKPOINT_OK`

## Debugging Note

During verification, I again hit the known Surefire discovery noise when Maven test and package flows were launched in parallel. The dump showed `NoClassDefFoundError` during test discovery, which matches concurrent target-directory mutation rather than a behavioral regression.

Sequential fresh reruns of `test` and `package` passed cleanly.

## What This Unlocks Next

With Step 20 in place, the shell metadata layer is now richer and more model-aware:

- focus and checkpoint are both prompt-aware
- slash-command state has clearer semantics
- later steps can build plan mode, diff, and recovery workflows on top of the same temporary-context mechanism

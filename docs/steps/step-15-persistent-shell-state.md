# Step 15 - Persistent Shell State

## Step Goal

Persist interactive shell focus and named checkpoints through SQLite so advanced shell workflows survive CLI restarts.

## What Was Done

1. Added a dedicated `ShellStateStore` abstraction for shell workspace metadata.
2. Added in-memory and SQLite-backed shell-state store implementations.
3. Added `SqliteShellStateRepository` with persisted focus and checkpoint tables.
4. Extended `SqliteSessionRepository` so checkpoint snapshots can reuse existing session persistence.
5. Switched slash commands from transient `ShellRuntimeState` to persisted `ShellStateStore`.
6. Verified that focus and checkpoints survive two separate packaged interactive CLI runs against the same SQLite database.

## Files Added or Changed

### Production Code

- `axercode-cli/src/main/java/com/axercode/cli/service/ShellStateStore.java`
- `axercode-cli/src/main/java/com/axercode/cli/service/InMemoryShellStateStore.java`
- `axercode-cli/src/main/java/com/axercode/cli/service/SqliteBackedShellStateStore.java`
- `axercode-cli/src/main/java/com/axercode/cli/service/InteractiveShellService.java`
- `axercode-cli/src/main/java/com/axercode/cli/shell/SlashCommandDispatcher.java`
- `axercode-cli/src/main/java/com/axercode/cli/config/StorageConfiguration.java`
- `axercode-storage-sqlite/src/main/java/com/axercode/storage/sqlite/shell/SqliteShellStateRepository.java`
- `axercode-storage-sqlite/src/main/java/com/axercode/storage/sqlite/session/SqliteSessionRepository.java`

### Removed

- `axercode-cli/src/main/java/com/axercode/cli/shell/ShellRuntimeState.java`

### Tests

- `axercode-storage-sqlite/src/test/java/com/axercode/storage/sqlite/shell/SqliteShellStateRepositoryTest.java`
- `axercode-cli/src/test/java/com/axercode/cli/service/SqliteBackedShellStateStoreTest.java`
- `axercode-cli/src/test/java/com/axercode/cli/shell/SlashCommandDispatcherTest.java`
- `axercode-cli/src/test/java/com/axercode/cli/service/InteractiveShellServiceTest.java`

### Documentation

- `docs/superpowers/specs/2026-04-18-axercode-persistent-shell-state-design.md`
- `docs/superpowers/plans/2026-04-18-axercode-step-15-persistent-shell-state.md`
- `docs/steps/step-15-persistent-shell-state.md`

## How It Was Implemented

## 1. Separate Shell Workspace State from Current Session State

The key design move in this step was introducing `ShellStateStore`.

That keeps two different concerns separate:

- `SessionStore` still owns the current active conversation
- `ShellStateStore` owns shell workspace metadata such as focus path and named checkpoints

This is cleaner than overloading the current-session repository with unrelated shell behavior.

## 2. Reuse Existing Session Persistence for Checkpoints

Named checkpoints are not stored as ad-hoc blobs.

Instead:

1. the checkpoint `SessionContext` is saved through `SqliteSessionRepository.saveSession(...)`
2. `SqliteShellStateRepository` stores the checkpoint name and referenced session id

This keeps session serialization logic in one place and avoids duplicating message persistence code.

## 3. New SQLite Tables

The storage module now creates:

- `shell_focus`
- `shell_checkpoints`

`shell_focus` stores one persisted focus path.

`shell_checkpoints` stores:

- checkpoint name
- referenced session id
- saved timestamp

The checkpoint session messages still live in the existing `sessions` and `session_messages` tables.

## 4. Slash Commands Now Use Persisted State

`SlashCommandDispatcher` now reads and writes through `ShellStateStore` instead of the old transient `ShellRuntimeState`.

That means these commands now survive restart:

- `/focus`
- `/focus clear`
- `/checkpoint <name>`
- `/checkpoints`
- `/restore <name>`
- `/status`

The shell command behavior looks the same to the user, but the backing state is now durable.

## 5. Production Wiring

`StorageConfiguration` now exposes `SqliteShellStateRepository`.

`SqliteBackedShellStateStore` is a Spring-managed production component, and `InteractiveShellService` now uses the injected `ShellStateStore` bean in production.

Tests still use `InMemoryShellStateStore` where persistence is unnecessary.

## Why This Design

The main design choice here was to add persistence without collapsing the shell into the storage layer.

This gives a better long-term shape:

- shell commands stay small
- SQLite details stay in repositories and stores
- future workspace features have a natural extension point

## Important Limitation

This step makes focus and checkpoints durable, but it does **not** yet make focus provider-aware.

That means:

- `/status` can show the restored focus path
- `/focus` survives restart
- but the focus path is not yet injected into the model prompt automatically

Also, checkpoint names are currently global within the local shell database. There is no namespacing or branch concept yet.

## TDD Evidence

This step followed red-green loops in three layers:

1. `SqliteShellStateRepositoryTest` failed first because the repository type and tables did not exist
2. `SqliteBackedShellStateStoreTest` failed next because the shell-state store boundary did not exist
3. `InteractiveShellServiceTest` then failed until the shell used the persisted store instead of transient runtime state

That progression kept the implementation grounded in the exact behavior we wanted: restart-safe focus and checkpoints.

## Verification Commands

The following commands were executed successfully in `D:\AeroCode1`:

```bat
scripts\mvn-jdk21.cmd -q -pl axercode-storage-sqlite,axercode-cli -am test "-Dtest=SqliteShellStateRepositoryTest,SqliteBackedShellStateStoreTest,SlashCommandDispatcherTest,InteractiveShellServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"
scripts\mvn-jdk21.cmd -q -pl axercode-storage-sqlite -am test
scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test
scripts\mvn-jdk21.cmd -q test
scripts\mvn-jdk21.cmd -q package
```

## Runtime Verification

The packaged CLI was verified across two separate interactive runs using the same SQLite database and the pinned JDK 21.

### First run

Saved:

- focus path `D:\AeroCode1`
- checkpoint `alpha`

### Second run

Observed:

- `/status` showed `Focus: D:\AeroCode1`
- `/checkpoints` listed `alpha`
- `/restore alpha` restored the checkpointed conversation
- `/history` showed:
  - `USER: hello`
  - `ASSISTANT: Hello! How can I assist you today?`

## Runtime Note

Attempting to pass Spring-style `--axercode.storage...` options through the packaged CLI failed because Picocli correctly rejected unknown command options first.

For runtime verification, JVM system properties were used instead:

- `-Daxercode.storage.database-file=...`
- `-Daxercode.shell.history-file=...`

That is not a product bug, just an important packaging/runtime detail for later configuration work.

## Why This Step Matters

This is the first point where advanced shell workflows feel durable instead of temporary.

AxerCode now keeps shell workspace intent across restarts, which is a real quality jump for local coding-agent usage.

## Next Step

Step 16 should build on this durable shell state and start making higher-level workflows more agent-aware, likely around plan-oriented execution or focus-aware prompting.

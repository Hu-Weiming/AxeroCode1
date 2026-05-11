# Step 9 - SQLite Session Persistence

## Step Goal

Persist the current CLI conversation session to SQLite so AxerCode can restore its latest conversation history across process restarts.

## What Was Done

1. Added a JDBC-based SQLite repository in `axercode-storage-sqlite`.
2. Added a CLI `SessionStore` abstraction so the shell no longer depends directly on the in-memory implementation.
3. Kept `InMemorySessionStore` as a lightweight non-Spring implementation for tests.
4. Added `SqliteBackedSessionStore` as the production `SessionStore` bean.
5. Added Spring Boot configuration for the SQLite database file path.
6. Verified the packaged CLI across two real runs against the same SQLite file and confirmed the second run restored prior history.

## Files Added or Changed

### Production Code

- `axercode-storage-sqlite/pom.xml`
- `axercode-storage-sqlite/src/main/java/com/axercode/storage/sqlite/session/SqliteSessionRepository.java`
- `axercode-cli/src/main/java/com/axercode/cli/service/SessionStore.java`
- `axercode-cli/src/main/java/com/axercode/cli/service/InMemorySessionStore.java`
- `axercode-cli/src/main/java/com/axercode/cli/service/SqliteBackedSessionStore.java`
- `axercode-cli/src/main/java/com/axercode/cli/service/InteractiveShellService.java`
- `axercode-cli/src/main/java/com/axercode/cli/config/AxerCodeStorageProperties.java`
- `axercode-cli/src/main/java/com/axercode/cli/config/StorageConfiguration.java`
- `axercode-cli/src/main/java/com/axercode/cli/bootstrap/AxerCodeCliApplication.java`
- `axercode-cli/src/main/resources/application.yml`

### Tests

- `axercode-storage-sqlite/src/test/java/com/axercode/storage/sqlite/session/SqliteSessionRepositoryTest.java`
- `axercode-cli/src/test/java/com/axercode/cli/service/SqliteBackedSessionStoreTest.java`

## How It Was Implemented

## 1. SQLite Repository

`SqliteSessionRepository` is a plain JDBC repository. It owns three tables:

- `sessions`
- `session_messages`
- `active_session`

That design keeps the persistence rules explicit:

- every saved session has a stable `SessionId`
- messages are stored in insertion order through `sequence_no`
- one row in `active_session` points to the CLI session that should be restored on the next launch

The repository exposes three operations:

- `loadCurrentSession()`
- `loadSession(SessionId)`
- `saveCurrentSession(SessionContext)`

This is enough for the CLI to both restore the current session and keep older sessions accessible by id.

## 2. CLI SessionStore Abstraction

The shell previously depended directly on `InMemorySessionStore`.

Step 9 introduces `SessionStore` so the shell depends on behavior instead:

- get current session
- replace current session
- reset current session

This keeps `InteractiveShellService` simple and lets tests continue using a fast in-memory implementation while production moves to SQLite.

## 3. SQLite-Backed CLI Session Store

`SqliteBackedSessionStore` is now the production Spring component.

On startup it:

- loads the current session from SQLite if one exists
- otherwise creates a fresh empty session and persists it immediately

On updates it:

- saves the full current session snapshot after each conversation turn
- creates a fresh persisted session on `/new`

That means Step 9 persistence is not theoretical. The interactive shell state now survives process exits.

## 4. Spring Boot Storage Configuration

`AxerCodeStorageProperties` adds a configurable database file:

`%USERPROFILE%\.axercode\data\axercode.db`

`StorageConfiguration` resolves that path and creates the `SqliteSessionRepository` bean.

`AxerCodeCliApplication` now enables these properties, so the packaged CLI gets a working SQLite session store by default.

## Why This Design

The main design choice was to avoid hard-wiring persistence logic into `InteractiveShellService`.

Instead:

- storage concerns live in `axercode-storage-sqlite`
- shell behavior still lives in `InteractiveShellService`
- the CLI only depends on the `SessionStore` interface

That keeps the layering clean and makes future session branching, checkpoints, or history browsing easier to add later.

## TDD Evidence

This step again followed red-green loops:

1. `SqliteSessionRepositoryTest` failed first because the repository did not exist.
2. `SqliteBackedSessionStoreTest` failed first because the CLI had no SQLite-backed session store.
3. Minimal repository and store code was added until both targeted test sets passed.

## Verification Commands

The following commands were executed successfully in `D:\AeroCode1`:

```bat
scripts\mvn-jdk21.cmd -q -pl axercode-storage-sqlite -am test "-Dtest=SqliteSessionRepositoryTest" "-Dsurefire.failIfNoSpecifiedTests=false"
scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test "-Dtest=SqliteBackedSessionStoreTest" "-Dsurefire.failIfNoSpecifiedTests=false"
scripts\mvn-jdk21.cmd -q -pl axercode-storage-sqlite -am test
scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test
scripts\mvn-jdk21.cmd -q test
scripts\mvn-jdk21.cmd -q -pl axercode-cli -am package
```

Real runtime verification also passed across two runs against the same SQLite file:

```bat
@'
hello persistence
/exit
'@ | "C:\Program Files\Java\jdk-21\bin\java.exe" "-Daxercode.storage.database-file=D:\AeroCode1\target\step9-data\axercode.db" "-Daxercode.shell.history-file=D:\AeroCode1\target\step9-data\cli.history" -jar D:\AeroCode1\axercode-cli\target\axercode-cli-0.1.0-SNAPSHOT.jar --interactive

@'
/history
/exit
'@ | "C:\Program Files\Java\jdk-21\bin\java.exe" "-Daxercode.storage.database-file=D:\AeroCode1\target\step9-data\axercode.db" "-Daxercode.shell.history-file=D:\AeroCode1\target\step9-data\cli.history" -jar D:\AeroCode1\axercode-cli\target\axercode-cli-0.1.0-SNAPSHOT.jar --interactive
```

Observed runtime result:

- the first run stored the conversation in SQLite
- the second run printed the previous `USER` and `ASSISTANT` messages with `/history`

## Why This Step Matters

This is the first point where AxerCode’s CLI session is durable instead of process-local.

It now supports:

- restoring the current conversation after restart
- keeping session message order intact in SQLite
- switching to a new session without losing the previous one
- clean separation between shell behavior and persistence implementation

## Next Step

Step 10 should build the first tool execution framework so the assistant can move from pure chat to controlled local actions such as file reads, directory listing, and shell execution.

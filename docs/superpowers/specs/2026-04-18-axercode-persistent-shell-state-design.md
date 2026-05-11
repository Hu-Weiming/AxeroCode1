# AxerCode Persistent Shell State Design

## Goal

Persist interactive shell workspace metadata so focus paths and named checkpoints survive CLI restarts instead of only existing for one process lifetime.

## Scope

This step covers:

- persisting shell focus path in SQLite
- persisting named checkpoints in SQLite
- restoring persisted focus path and checkpoints on the next interactive run
- introducing a shell-state store abstraction so the shell does not own persistence details

This step does **not** cover:

- git-backed branches or diffs
- provider-aware focus prompt injection
- distributed or multi-user shell state
- desktop or Web surfaces

## Problem

Step 14 introduced useful advanced commands:

- `/focus`
- `/checkpoint`
- `/checkpoints`
- `/restore`

But their state lives only in `ShellRuntimeState`, which is recreated for every interactive run.

That means:

- checkpoints disappear on restart
- focus disappears on restart
- shell workflows feel temporary even though the project already has SQLite persistence available

## Approaches Considered

### 1. Keep shell runtime state purely in memory

Simple, but it does not solve the actual persistence problem.

### 2. Add a dedicated `ShellStateStore` abstraction with a SQLite-backed implementation

Keeps shell command logic isolated from persistence details and mirrors the existing `SessionStore` pattern.

### 3. Push focus and checkpoint behavior directly into `SqliteSessionRepository`

Would reduce the number of types, but it mixes shell workspace metadata with current-session persistence and makes later evolution harder.

## Recommended Design

Use approach 2.

Add:

1. a `ShellStateStore` abstraction in `axercode-cli`
2. an in-memory implementation for tests
3. a SQLite-backed implementation for production
4. a `SqliteShellStateRepository` in the storage module

## Responsibilities

### `ShellStateStore`

Owns:

- current focus path
- saving a named checkpoint
- loading a named checkpoint
- listing checkpoint names
- counting checkpoints

It should not own the current active session. That remains the responsibility of `SessionStore`.

### `SqliteShellStateRepository`

Owns the low-level SQLite tables and queries for:

- persisted focus path
- checkpoint name to session id mapping

It should reuse `SqliteSessionRepository` for storing and loading `SessionContext` snapshots rather than re-implementing session serialization.

## Storage Model

Add two tables:

- `shell_focus`
- `shell_checkpoints`

`shell_focus` stores a singleton persisted path.

`shell_checkpoints` stores:

- checkpoint name
- referenced session id
- save timestamp

Checkpoint sessions themselves should be persisted through the existing session tables.

## Why This Design

The key design choice is separating:

- current session persistence
- shell workspace metadata persistence

That keeps the CLI shell thin and avoids turning one repository into a grab bag of unrelated concerns.

It also gives later features a clean place to extend workspace state without rewriting the shell command layer again.

## Testing Strategy

Add tests for:

- repository-level focus and checkpoint persistence
- CLI shell-state store restore behavior
- interactive shell behavior across two shell instances sharing one SQLite database

## Outcome

After this step, `/focus`, `/checkpoint`, `/checkpoints`, and `/restore` will become durable shell workflows instead of temporary in-memory conveniences.

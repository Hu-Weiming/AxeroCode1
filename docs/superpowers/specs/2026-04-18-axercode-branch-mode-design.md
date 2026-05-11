# AxerCode Session Branching Design

## Goal

Add a first-pass `/branch` workflow so the interactive shell can fork the current conversation into named session branches and switch between them later.

## Scope

This step adds:

- named session branches
- active branch persistence across restarts
- slash commands for branch creation, switching, listing, and status
- temporary prompt shaping so the model can see the active branch name

This step does not add Git integration or code-branch management.

## Recommended Approach

Treat branches as named aliases to persisted `SessionContext` snapshots.

Behavior:

- if `/branch <name>` targets an existing branch, load its session and switch to it
- if the branch does not exist, clone the current session into a new session id, save it under that name, and switch to it
- keep an active branch name in shell state
- show the active branch in `/status`
- inject the active branch into temporary provider context

Why this approach:

- it matches the current SQLite-backed session architecture
- it avoids changing the agent or session persistence model
- it gives users an easy way to branch conversations without losing the main line

## Design

### 1. Session cloning

Branch creation must not reuse the same `SessionId`.

It must also avoid reusing the same message ids because `message_id` is globally unique in SQLite.

So the branch feature needs a small cloning utility that:

- creates a new `SessionId`
- copies each message with a new message id
- preserves role, content, and timestamp

### 2. Branch storage

Shell state will own:

- branch name -> session id mapping
- active branch name

Both should persist in SQLite and work in memory for tests.

### 3. Slash command behavior

The shell will support:

- `/branch`
- `/branch status`
- `/branch list`
- `/branch <name>`

`/branch <name>` is dual-purpose:

- switch if it already exists
- create-and-switch if it does not

`/new` should clear the active branch because it creates a fresh unnamed session.

### 4. Prompt shaping

When an active branch exists, `ShellContextAugmenter` should add a temporary `SYSTEM` message describing the branch name.

This keeps branch context visible to the model without polluting session history.

## Files Expected To Change

- `D:\AeroCode1\axercode-core\src\main\java\com\axercode\core\session\SessionContextBrancher.java`
- `D:\AeroCode1\axercode-core\src\test\java\com\axercode\core\session\SessionContextBrancherTest.java`
- `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\ShellStateStore.java`
- `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\InMemoryShellStateStore.java`
- `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\SqliteBackedShellStateStore.java`
- `D:\AeroCode1\axercode-storage-sqlite\src\main\java\com\axercode\storage\sqlite\shell\SqliteShellStateRepository.java`
- `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\shell\SlashCommandDispatcher.java`
- `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\ShellContextAugmenter.java`
- `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\InteractiveShellService.java`
- `D:\AeroCode1\axercode-cli\src\test\java\com\axercode\cli\shell\SlashCommandDispatcherTest.java`
- `D:\AeroCode1\axercode-cli\src\test\java\com\axercode\cli\service\SqliteBackedShellStateStoreTest.java`
- `D:\AeroCode1\axercode-cli\src\test\java\com\axercode\cli\service\CliChatServiceTest.java`

## Verification Strategy

- core tests for session cloning into a new branch session
- shell-state tests for branch persistence
- slash-command tests for create, switch, list, and status behavior
- CLI service tests proving active branch prompt context is injected but not persisted
- full Maven test and package verification on JDK 21

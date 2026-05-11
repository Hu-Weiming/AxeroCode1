# Step 24 Session Branching Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add first-pass session branching through `/branch` with persistence, switching, and active-branch prompt context.

**Architecture:** Introduce a small core session-cloning utility, persist named branch mappings in shell state, add `/branch` slash commands, and inject the active branch name as temporary provider context.

**Tech Stack:** Java 21, Spring Boot 3.3.4, SQLite JDBC, JUnit 5, Picocli, JLine 3

---

### Task 1: Add a core session-branch cloning utility

**Files:**
- Create: `D:\AeroCode1\axercode-core\src\main\java\com\axercode\core\session\SessionContextBrancher.java`
- Create: `D:\AeroCode1\axercode-core\src\test\java\com\axercode\core\session\SessionContextBrancherTest.java`

- [ ] Write failing tests proving a branched session gets a new session id and new message ids while preserving message content.
- [ ] Run `scripts\mvn-jdk21.cmd -q -pl axercode-core -am test "-Dtest=SessionContextBrancherTest" "-Dsurefire.failIfNoSpecifiedTests=false"` and confirm red.
- [ ] Implement the minimal cloning utility.
- [ ] Re-run the same command and confirm green.

### Task 2: Add branch persistence to shell state

**Files:**
- Modify: `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\ShellStateStore.java`
- Modify: `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\InMemoryShellStateStore.java`
- Modify: `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\SqliteBackedShellStateStore.java`
- Modify: `D:\AeroCode1\axercode-storage-sqlite\src\main\java\com\axercode\storage\sqlite\shell\SqliteShellStateRepository.java`
- Modify: `D:\AeroCode1\axercode-cli\src\test\java\com\axercode\cli\service\SqliteBackedShellStateStoreTest.java`

- [ ] Write failing tests for branch persistence and active-branch reload.
- [ ] Run the targeted shell-state test command and confirm red.
- [ ] Implement the minimal persistence changes.
- [ ] Re-run the same command and confirm green.

### Task 3: Add `/branch` command behavior

**Files:**
- Modify: `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\shell\SlashCommandDispatcher.java`
- Modify: `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\InteractiveShellService.java`
- Modify: `D:\AeroCode1\axercode-cli\src\test\java\com\axercode\cli\shell\SlashCommandDispatcherTest.java`

- [ ] Write failing tests for `/branch`, `/branch status`, `/branch list`, and `/branch <name>`.
- [ ] Run the targeted slash-command test command and confirm red.
- [ ] Implement create-and-switch plus switch-existing behavior.
- [ ] Re-run the same command and confirm green.

### Task 4: Inject active branch prompt context

**Files:**
- Modify: `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\ShellContextAugmenter.java`
- Modify: `D:\AeroCode1\axercode-cli\src\test\java\com\axercode\cli\service\CliChatServiceTest.java`

- [ ] Write failing tests proving active branch guidance is prepended and stripped back out.
- [ ] Run the targeted CLI service test command and confirm red.
- [ ] Implement the minimal branch-aware temporary system message.
- [ ] Re-run the same command and confirm green.

### Task 5: Verify and document Step 24

**Files:**
- Create: `D:\AeroCode1\docs\steps\step-24-branching.md`

- [ ] Run `scripts\mvn-jdk21.cmd -q -pl axercode-core -am test`.
- [ ] Run `scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test`.
- [ ] Run `scripts\mvn-jdk21.cmd -q test`.
- [ ] Run `scripts\mvn-jdk21.cmd -q package`.
- [ ] Run the packaged CLI interactively, create a branch, restart, and confirm `/status` shows the active branch.
- [ ] Write the Step 24 learning summary.

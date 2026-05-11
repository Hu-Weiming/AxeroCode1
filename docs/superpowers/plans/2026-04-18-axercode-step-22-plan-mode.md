# Step 22 Plan Mode Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a first-pass shell-level Plan Mode with slash-command control, persistence, and temporary prompt shaping.

**Architecture:** Extend shell state with a persisted plan-mode flag, expose `/plan` commands in the interactive shell, and let `ShellContextAugmenter` inject a temporary planning instruction whenever the mode is enabled.

**Tech Stack:** Java 21, Spring Boot 3.3.4, SQLite JDBC, JUnit 5, Picocli, JLine 3

---

### Task 1: Add plan-mode state to shell storage

**Files:**
- Modify: `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\ShellStateStore.java`
- Modify: `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\InMemoryShellStateStore.java`
- Modify: `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\SqliteBackedShellStateStore.java`
- Modify: `D:\AeroCode1\axercode-storage-sqlite\src\main\java\com\axercode\storage\sqlite\shell\SqliteShellStateRepository.java`
- Modify: `D:\AeroCode1\axercode-cli\src\test\java\com\axercode\cli\service\SqliteBackedShellStateStoreTest.java`

- [ ] Write failing tests for persisted plan mode reload.
- [ ] Run the targeted shell-state test command and confirm red.
- [ ] Implement the minimal store and repository changes.
- [ ] Re-run the same test command and confirm green.

### Task 2: Add `/plan` command behavior

**Files:**
- Modify: `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\shell\SlashCommandDispatcher.java`
- Modify: `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\InteractiveShellService.java`
- Modify: `D:\AeroCode1\axercode-cli\src\test\java\com\axercode\cli\shell\SlashCommandDispatcherTest.java`

- [ ] Write failing tests for `/plan on`, `/plan off`, `/plan status`, and `/status`.
- [ ] Run the targeted slash-command test command and confirm red.
- [ ] Implement the minimal command and completion changes.
- [ ] Re-run the same test command and confirm green.

### Task 3: Inject temporary plan-mode prompt guidance

**Files:**
- Modify: `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\ShellContextAugmenter.java`
- Modify: `D:\AeroCode1\axercode-cli\src\test\java\com\axercode\cli\service\CliChatServiceTest.java`

- [ ] Write failing tests proving plan-mode guidance is prepended and stripped back out.
- [ ] Run the targeted CLI service test command and confirm red.
- [ ] Implement the minimal temporary system-message injection.
- [ ] Re-run the same test command and confirm green.

### Task 4: Verify and document Step 22

**Files:**
- Create: `D:\AeroCode1\docs\steps\step-22-plan-mode.md`

- [ ] Run `scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test`.
- [ ] Run `scripts\mvn-jdk21.cmd -q test`.
- [ ] Run `scripts\mvn-jdk21.cmd -q package`.
- [ ] Run the packaged CLI interactively, enable plan mode, and confirm status output survives restart.
- [ ] Write the Step 22 learning summary.

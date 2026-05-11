# Step 20 Checkpoint-Aware Prompting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the active checkpoint part of temporary model context while keeping stored session history clean.

**Architecture:** Extend `ShellStateStore` with active-checkpoint metadata, persist it in SQLite, update slash commands to maintain it, and let `ShellContextAugmenter` inject checkpoint-aware system guidance alongside the existing focus message.

**Tech Stack:** Java 21, Spring Boot 3.3.4, SQLite JDBC, JUnit 5

---

### Task 1: Add active-checkpoint state to the shell store

**Files:**
- Modify: `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\ShellStateStore.java`
- Modify: `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\InMemoryShellStateStore.java`
- Modify: `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\SqliteBackedShellStateStore.java`
- Modify: `D:\AeroCode1\axercode-storage-sqlite\src\main\java\com\axercode\storage\sqlite\shell\SqliteShellStateRepository.java`
- Modify: `D:\AeroCode1\axercode-cli\src\test\java\com\axercode\cli\service\SqliteBackedShellStateStoreTest.java`

- [ ] Write failing tests for active checkpoint persistence and reload.
- [ ] Run the targeted shell-state test and confirm red.
- [ ] Implement the minimal active-checkpoint store/repository behavior.
- [ ] Re-run the targeted test and confirm green.

### Task 2: Wire active checkpoint into slash commands

**Files:**
- Modify: `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\shell\SlashCommandDispatcher.java`
- Modify: `D:\AeroCode1\axercode-cli\src\test\java\com\axercode\cli\shell\SlashCommandDispatcherTest.java`

- [ ] Write failing tests for `/status`, `/checkpoint`, `/restore`, and `/new` active-checkpoint behavior.
- [ ] Run the targeted slash-command test and confirm red.
- [ ] Implement the minimal command behavior.
- [ ] Re-run the targeted test and confirm green.

### Task 3: Inject checkpoint context into prompt shaping

**Files:**
- Modify: `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\ShellContextAugmenter.java`
- Modify: `D:\AeroCode1\axercode-cli\src\test\java\com\axercode\cli\service\CliChatServiceTest.java`

- [ ] Write failing tests proving active checkpoint guidance is prepended and stripped back out after the turn.
- [ ] Run the targeted CLI service test and confirm red.
- [ ] Implement the minimal checkpoint-aware prompt message generation.
- [ ] Re-run the targeted test and confirm green.

### Task 4: Verify and document Step 20

**Files:**
- Create: `D:\AeroCode1\docs\steps\step-20-checkpoint-aware-prompting.md`

- [ ] Run `scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test`.
- [ ] Run `scripts\mvn-jdk21.cmd -q test`.
- [ ] Run `scripts\mvn-jdk21.cmd -q package`.
- [ ] Run the packaged CLI on JDK 21 and confirm prompt execution still works after restoring a checkpoint.
- [ ] Write the Step 20 learning summary.

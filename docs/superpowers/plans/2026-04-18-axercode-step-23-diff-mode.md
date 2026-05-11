# Step 23 Session Diff Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a first-pass `/diff` command that compares the current session against a checkpoint or branch.

**Architecture:** Introduce a tiny core session differ that compares sessions by shared prefix and tail messages, then expose it through slash commands with lightweight CLI formatting.

**Tech Stack:** Java 21, Spring Boot 3.3.4, SQLite JDBC, JUnit 5, JLine 3

---

### Task 1: Add a reusable core session differ

**Files:**
- Create: `D:\AeroCode1\axercode-core\src\main\java\com\axercode\core\session\SessionDiff.java`
- Create: `D:\AeroCode1\axercode-core\src\main\java\com\axercode\core\session\SessionContextDiffer.java`
- Create: `D:\AeroCode1\axercode-core\src\test\java\com\axercode\core\session\SessionContextDifferTest.java`

- [ ] Write failing tests for identical sessions and diverging tail messages.
- [ ] Run `scripts\mvn-jdk21.cmd -q -pl axercode-core -am test "-Dtest=SessionContextDifferTest" "-Dsurefire.failIfNoSpecifiedTests=false"` and confirm red.
- [ ] Implement the minimal differ.
- [ ] Re-run the same command and confirm green.

### Task 2: Add `/diff` slash command behavior

**Files:**
- Modify: `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\shell\SlashCommandDispatcher.java`
- Modify: `D:\AeroCode1\axercode-cli\src\test\java\com\axercode\cli\shell\SlashCommandDispatcherTest.java`

- [ ] Write failing tests for `/diff`, `/diff checkpoint <name>`, `/diff branch <name>`, and no-target guidance.
- [ ] Run `scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test "-Dtest=SlashCommandDispatcherTest" "-Dsurefire.failIfNoSpecifiedTests=false"` and confirm red.
- [ ] Implement the minimal diff command and formatter.
- [ ] Re-run the same command and confirm green.

### Task 3: Add JLine discoverability

**Files:**
- Modify: `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\InteractiveShellService.java`
- Modify: `D:\AeroCode1\axercode-cli\src\test\java\com\axercode\cli\service\InteractiveShellServiceTest.java`

- [ ] Write a failing test that proves `/help` exposes `/diff`.
- [ ] Run `scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test "-Dtest=InteractiveShellServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"` and confirm red if needed.
- [ ] Add `/diff` to the JLine completer and help surface.
- [ ] Re-run the targeted command and confirm green.

### Task 4: Verify and document Step 23

**Files:**
- Create: `D:\AeroCode1\docs\steps\step-23-diff-mode.md`

- [ ] Run `scripts\mvn-jdk21.cmd -q -pl axercode-core -am test`.
- [ ] Run `scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test`.
- [ ] Run `scripts\mvn-jdk21.cmd -q test`.
- [ ] Run `scripts\mvn-jdk21.cmd -q package`.
- [ ] Run the packaged CLI, create a checkpoint, and verify `/diff` against it.
- [ ] Write the Step 23 learning summary.

# AxerCode Step 14 Advanced Slash Commands Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an extensible slash-command dispatcher to the interactive CLI and ship the first advanced shell commands for status, focus, and checkpoint workflows.

**Architecture:** Keep the JLine shell loop in `InteractiveShellService`, but move slash command behavior into a dedicated dispatcher backed by a per-run `ShellRuntimeState`. The current session remains in `SessionStore`, while focus and named checkpoints live in transient runtime state for this step.

**Tech Stack:** Java 21, Spring Boot 3.3.4, JLine 3, JUnit 5

---

### Task 1: Lock advanced slash command behavior with failing tests

**Files:**
- Create: `axercode-cli/src/test/java/com/axercode/cli/shell/SlashCommandDispatcherTest.java`
- Modify: `axercode-cli/src/test/java/com/axercode/cli/service/InteractiveShellServiceTest.java`

- [ ] **Step 1: Add a failing dispatcher test for `/status` and `/focus`**
- [ ] **Step 2: Add a failing dispatcher test for `/checkpoint`, `/checkpoints`, and `/restore`**
- [ ] **Step 3: Add a failing interactive shell test that drives the new commands through the shell loop**
- [ ] **Step 4: Run the targeted CLI tests and confirm they fail for the expected missing-type or missing-behavior reasons**

### Task 2: Add slash command runtime state and dispatcher

**Files:**
- Create: `axercode-cli/src/main/java/com/axercode/cli/shell/ShellRuntimeState.java`
- Create: `axercode-cli/src/main/java/com/axercode/cli/shell/ShellCommandResult.java`
- Create: `axercode-cli/src/main/java/com/axercode/cli/shell/SlashCommandDispatcher.java`

- [ ] **Step 1: Implement `ShellRuntimeState` for focus path and named checkpoints**
- [ ] **Step 2: Implement `ShellCommandResult` for slash command outputs and continue/stop control**
- [ ] **Step 3: Implement `SlashCommandDispatcher` with `/status`, `/focus`, `/checkpoint`, `/checkpoints`, and `/restore`**
- [ ] **Step 4: Re-run targeted dispatcher tests and confirm they pass**

### Task 3: Integrate the dispatcher into the interactive shell

**Files:**
- Modify: `axercode-cli/src/main/java/com/axercode/cli/service/InteractiveShellService.java`

- [ ] **Step 1: Replace the hardcoded slash-command switch with delegation to `SlashCommandDispatcher`**
- [ ] **Step 2: Create a fresh `ShellRuntimeState` per interactive run**
- [ ] **Step 3: Update `/help` output and JLine completer entries for the new commands**
- [ ] **Step 4: Re-run the interactive shell tests and confirm they pass**

### Task 4: Verify and document the step

**Files:**
- Create: `docs/steps/step-14-advanced-slash-commands.md`

- [ ] **Step 1: Run `axercode-cli` tests**
- [ ] **Step 2: Run the full reactor tests**
- [ ] **Step 3: Package the project**
- [ ] **Step 4: Run one interactive shell script to verify slash-command output**
- [ ] **Step 5: Write the learning summary and Step 14 result block**

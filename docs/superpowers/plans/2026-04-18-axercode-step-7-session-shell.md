# AxerCode Step 7 Session Shell Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a session-aware interactive CLI shell with in-memory multi-turn context while keeping the existing one-shot prompt mode intact.

**Architecture:** This step extends the CLI application rather than replacing it. A lightweight in-memory session store and interactive shell service will sit above `CliChatService`, letting the chat service assemble `ProviderRequest` objects from accumulated `SessionContext` state while the Picocli command chooses between one-shot mode and interactive mode.

**Tech Stack:** Java 21, Spring Boot 3.3.4, Picocli 4.7.7, JUnit 5, Spring Boot Test

---

### Task 1: Add multi-turn chat service tests

**Files:**
- Modify: `axercode-cli/src/test/java/com/axercode/cli/service/CliChatServiceTest.java`
- Create: `axercode-cli/src/main/java/com/axercode/cli/service/CliChatTurn.java`

- [ ] **Step 1: Write failing tests for continuing a conversation from existing session context and appending assistant replies**
- [ ] **Step 2: Run targeted tests and verify they fail because session-aware chat methods do not exist**
- [ ] **Step 3: Implement the minimal session-aware chat result model and service methods**
- [ ] **Step 4: Re-run the targeted tests and verify they pass**

### Task 2: Add the in-memory interactive shell with TDD

**Files:**
- Create: `axercode-cli/src/test/java/com/axercode/cli/service/InteractiveShellServiceTest.java`
- Create: `axercode-cli/src/main/java/com/axercode/cli/service/InMemorySessionStore.java`
- Create: `axercode-cli/src/main/java/com/axercode/cli/service/InteractiveShellService.java`

- [ ] **Step 1: Write failing tests for `/help`, `/history`, `/new`, `/exit`, and multi-turn prompt flow**
- [ ] **Step 2: Run the targeted tests and verify they fail because the interactive shell classes do not exist**
- [ ] **Step 3: Implement the minimal in-memory session store and interactive shell loop**
- [ ] **Step 4: Re-run the targeted tests and verify they pass**

### Task 3: Extend the Picocli command to dispatch interactive mode

**Files:**
- Modify: `axercode-cli/src/test/java/com/axercode/cli/command/AxerCodeCliCommandTest.java`
- Modify: `axercode-cli/src/main/java/com/axercode/cli/command/AxerCodeCliCommand.java`

- [ ] **Step 1: Write failing command tests for `--interactive` dispatch and missing-mode validation**
- [ ] **Step 2: Run the targeted tests and verify they fail**
- [ ] **Step 3: Implement the minimal command changes**
- [ ] **Step 4: Re-run the targeted tests and verify they pass**

### Task 4: Verify the CLI path and write the step summary

**Files:**
- Create: `docs/steps/step-7-session-shell.md`

- [ ] **Step 1: Run `axercode-cli` tests**
- [ ] **Step 2: Run the full reactor tests**
- [ ] **Step 3: Run the CLI jar in interactive mode with scripted input**
- [ ] **Step 4: Write the learning summary**

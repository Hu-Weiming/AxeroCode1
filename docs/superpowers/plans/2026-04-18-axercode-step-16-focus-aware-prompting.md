# AxerCode Step 16 Focus-Aware Prompting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make persisted `/focus` actually influence agent turns by injecting temporary system context into provider requests without polluting stored session history.

**Architecture:** Keep `ConversationAgent` unchanged and implement focus handling in the CLI layer through a dedicated context augmenter. The CLI will prepend temporary `SYSTEM` messages before delegating to the agent, then strip them out of the returned session before persisting it back to `SessionStore`.

**Tech Stack:** Java 21, Spring Boot 3.3.4, JUnit 5

---

### Task 1: Lock focus-aware behavior with failing tests

**Files:**
- Modify: `axercode-cli/src/test/java/com/axercode/cli/service/CliChatServiceTest.java`
- Modify: `axercode-cli/src/test/java/com/axercode/cli/service/InteractiveShellServiceTest.java`

- [ ] **Step 1: Add a failing `CliChatService` test that expects a focus system message to be prepended before the agent call**
- [ ] **Step 2: Add a failing `CliChatService` test that expects the returned session to exclude the temporary focus system message**
- [ ] **Step 3: Add a failing interactive shell test that sets `/focus`, sends a prompt, and verifies the shared agent saw the injected system message**
- [ ] **Step 4: Run the targeted CLI tests and confirm they fail for the expected missing-behavior reasons**

### Task 2: Add reusable focus context augmentation

**Files:**
- Create: `axercode-cli/src/main/java/com/axercode/cli/service/ShellContextAugmenter.java`
- Modify: `axercode-cli/src/main/java/com/axercode/cli/service/CliChatService.java`

- [ ] **Step 1: Implement `ShellContextAugmenter` to build temporary system messages from `ShellStateStore`**
- [ ] **Step 2: Update `CliChatService` to prepend temporary focus messages before calling the agent**
- [ ] **Step 3: Strip the temporary messages back out of the returned session before creating `CliChatTurn`**
- [ ] **Step 4: Re-run the targeted `CliChatService` tests and confirm they pass**

### Task 3: Verify integration through the shell

**Files:**
- Modify: `axercode-cli/src/main/java/com/axercode/cli/service/InteractiveShellService.java`
- Modify: `axercode-cli/src/test/java/com/axercode/cli/service/InteractiveShellServiceTest.java`

- [ ] **Step 1: Ensure the shell and chat service share the same `ShellStateStore` in interactive tests**
- [ ] **Step 2: Re-run the targeted interactive shell tests and confirm focus now affects later prompt turns**

### Task 4: Verify and document the step

**Files:**
- Create: `docs/steps/step-16-focus-aware-prompting.md`

- [ ] **Step 1: Run `axercode-cli` tests**
- [ ] **Step 2: Run the full reactor tests**
- [ ] **Step 3: Package the project**
- [ ] **Step 4: Run one packaged interactive verification showing `/focus` survives and influences the next prompt turn**
- [ ] **Step 5: Write the learning summary and Step 16 result block**

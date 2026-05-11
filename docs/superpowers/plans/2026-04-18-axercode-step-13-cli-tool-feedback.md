# AxerCode Step 13 CLI Tool Feedback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Surface executed tool results in the CLI so users can see tool activity in both one-shot and interactive flows.

**Architecture:** Keep the agent contract stable and extend the CLI result model instead. Add a small formatter in `axercode-cli` that renders `ToolExecutionResult` items, then reuse it from the interactive shell and the one-shot command entry point.

**Tech Stack:** Java 21, Spring Boot 3.3.4, Picocli, JLine, JUnit 5

---

### Task 1: Lock the new CLI result behavior with tests

**Files:**
- Modify: `axercode-cli/src/test/java/com/axercode/cli/service/CliChatServiceTest.java`
- Modify: `axercode-cli/src/test/java/com/axercode/cli/service/InteractiveShellServiceTest.java`
- Modify: `axercode-cli/src/test/java/com/axercode/cli/command/AxerCodeCliCommandTest.java`

- [ ] **Step 1: Add a failing `CliChatService` test that expects tool results to survive the agent-to-CLI boundary**
- [ ] **Step 2: Add a failing interactive shell test that expects tool feedback to be printed before the assistant reply**
- [ ] **Step 3: Add a failing one-shot command test that expects tool feedback to be printed before the final reply**
- [ ] **Step 4: Run the targeted CLI tests and confirm they fail for the expected reason**

### Task 2: Add CLI-side tool feedback formatting

**Files:**
- Modify: `axercode-cli/src/main/java/com/axercode/cli/service/CliChatTurn.java`
- Modify: `axercode-cli/src/main/java/com/axercode/cli/service/CliChatService.java`
- Create: `axercode-cli/src/main/java/com/axercode/cli/service/ToolFeedbackFormatter.java`

- [ ] **Step 1: Extend `CliChatTurn` to carry the ordered list of tool results**
- [ ] **Step 2: Update `CliChatService` to preserve `toolResults` from `AgentConversationTurn`**
- [ ] **Step 3: Implement `ToolFeedbackFormatter` to render stable human-readable tool blocks**
- [ ] **Step 4: Re-run targeted `CliChatService` tests and confirm they pass**

### Task 3: Wire tool feedback into both CLI entry points

**Files:**
- Modify: `axercode-cli/src/main/java/com/axercode/cli/service/InteractiveShellService.java`
- Modify: `axercode-cli/src/main/java/com/axercode/cli/command/AxerCodeCliCommand.java`

- [ ] **Step 1: Print formatted tool feedback in the interactive shell before the final assistant reply**
- [ ] **Step 2: Print formatted tool feedback in one-shot `--prompt` mode before the final assistant reply**
- [ ] **Step 3: Keep empty tool lists silent so normal turns stay compact**
- [ ] **Step 4: Re-run targeted shell and command tests and confirm they pass**

### Task 4: Verify and document the step

**Files:**
- Create: `docs/steps/step-13-cli-tool-feedback.md`

- [ ] **Step 1: Run `axercode-cli` tests**
- [ ] **Step 2: Run the full reactor tests**
- [ ] **Step 3: Package the project**
- [ ] **Step 4: Write the learning summary and Step 13 result block**

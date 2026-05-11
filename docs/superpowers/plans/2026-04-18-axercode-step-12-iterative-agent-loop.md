# AxerCode Step 12 Iterative Agent Loop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the current tool-calling agent into a bounded iterative loop that can handle multiple rounds of tool calls and preserve failure observations.

**Architecture:** Keep the orchestration inside `axercode-agent` by evolving `ToolCallingAgent` into an explicit iterative loop with a constructor-supplied `maxToolRounds`. Leave provider schema mapping untouched and keep CLI changes limited to agent bean construction.

**Tech Stack:** Java 21, JUnit 5, existing provider and tool modules

---

### Task 1: Add iterative loop tests to the agent module

**Files:**
- Modify: `axercode-agent/src/test/java/com/axercode/agent/ToolCallingAgentTest.java`

- [ ] **Step 1: Write a failing test for multiple sequential tool rounds before final completion**
- [ ] **Step 2: Write a failing test for tool failure observations being preserved and sent back to the provider**
- [ ] **Step 3: Write a failing test for stopping when the maximum tool-round limit is reached**
- [ ] **Step 4: Run the targeted agent tests and verify they fail because the current agent only supports one round**

### Task 2: Implement the bounded iterative loop

**Files:**
- Modify: `axercode-agent/src/main/java/com/axercode/agent/ToolCallingAgent.java`

- [ ] **Step 1: Add a configurable `maxToolRounds` constructor parameter**
- [ ] **Step 2: Replace the single extra provider call with an explicit iterative loop**
- [ ] **Step 3: Preserve tool failure results as `TOOL` messages and continue the loop**
- [ ] **Step 4: Return a clear assistant message when the max round count is reached**
- [ ] **Step 5: Re-run the targeted agent tests and verify they pass**

### Task 3: Update CLI wiring to the new agent constructor

**Files:**
- Modify: `axercode-cli/src/main/java/com/axercode/cli/config/ToolConfiguration.java`
- Modify: `axercode-cli/src/test/java/com/axercode/cli/service/CliChatServiceTest.java`
- Modify: `axercode-cli/src/test/java/com/axercode/cli/service/InteractiveShellServiceTest.java`

- [ ] **Step 1: Update the CLI bean configuration to create the agent with a stable default max round count**
- [ ] **Step 2: Re-run the targeted CLI tests and verify they still pass**

### Task 4: Verify and document the step

**Files:**
- Create: `docs/steps/step-12-iterative-agent-loop.md`

- [ ] **Step 1: Run `axercode-agent` tests**
- [ ] **Step 2: Run `axercode-cli` tests**
- [ ] **Step 3: Run the full reactor tests**
- [ ] **Step 4: Package the project**
- [ ] **Step 5: Write the learning summary and Step 12 result block**

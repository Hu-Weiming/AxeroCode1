# AxerCode Step 11 Tool Calling Agent Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the first real tool-aware agent execution path so provider `tool_calls` are executed locally and fed back into the conversation before producing a final reply.

**Architecture:** Keep the loop logic inside `axercode-agent` with a lightweight `ConversationAgent` abstraction and `ToolCallingAgent` implementation. Keep `axercode-tools` reusable and plain, then wire concrete tool beans and the agent from the CLI module. Limit the loop to one tool-execution round in this step.

**Tech Stack:** Java 21, JUnit 5, existing provider and tool modules, Spring Boot bean wiring from the CLI module

---

### Task 1: Add agent module tests and result models

**Files:**
- Modify: `axercode-agent/pom.xml`
- Create: `axercode-agent/src/test/java/com/axercode/agent/ToolCallingAgentTest.java`
- Create: `axercode-agent/src/main/java/com/axercode/agent/ConversationAgent.java`
- Create: `axercode-agent/src/main/java/com/axercode/agent/AgentConversationTurn.java`

- [ ] **Step 1: Add JUnit support to the agent module**
- [ ] **Step 2: Write a failing test for a normal provider completion path**
- [ ] **Step 3: Write a failing test for `tool_calls -> tool execution -> second provider request`**
- [ ] **Step 4: Run the targeted agent tests and verify they fail because the agent classes do not exist**
- [ ] **Step 5: Implement the minimal agent result and interface types**
- [ ] **Step 6: Re-run the targeted tests and keep them failing only on missing execution logic**

### Task 2: Implement the first tool-calling agent loop

**Files:**
- Create: `axercode-agent/src/main/java/com/axercode/agent/ToolCallingAgent.java`

- [ ] **Step 1: Implement the minimal single-request completion path**
- [ ] **Step 2: Implement tool execution through `ToolExecutor`**
- [ ] **Step 3: Append tool observations as `TOOL` messages**
- [ ] **Step 4: Implement the second provider call for the final reply**
- [ ] **Step 5: Handle nested `tool_calls` with a friendly placeholder instead of a recursive loop**
- [ ] **Step 6: Re-run the targeted agent tests and verify they pass**

### Task 3: Wire the agent into the CLI

**Files:**
- Modify: `axercode-cli/src/main/java/com/axercode/cli/service/CliChatService.java`
- Modify: `axercode-cli/src/test/java/com/axercode/cli/service/CliChatServiceTest.java`
- Create: `axercode-cli/src/main/java/com/axercode/cli/config/ToolConfiguration.java`

- [ ] **Step 1: Write failing CLI tests for agent delegation and model selection**
- [ ] **Step 2: Run the targeted CLI tests and verify they fail**
- [ ] **Step 3: Change `CliChatService` to delegate to `ConversationAgent`**
- [ ] **Step 4: Register built-in tools, `ToolRegistry`, `ToolExecutor`, and `ConversationAgent` as CLI beans**
- [ ] **Step 5: Re-run the targeted CLI tests and verify they pass**

### Task 4: Verify and document the step

**Files:**
- Create: `docs/steps/step-11-tool-calling-agent.md`

- [ ] **Step 1: Run `axercode-agent` tests**
- [ ] **Step 2: Run `axercode-cli` tests**
- [ ] **Step 3: Run the full reactor tests**
- [ ] **Step 4: Package the project**
- [ ] **Step 5: Write the learning summary and Step 11 result block**

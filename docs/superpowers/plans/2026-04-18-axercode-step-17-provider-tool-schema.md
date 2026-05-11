# AxerCode Step 17 Provider Tool Schema Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Pass structured tool definitions from the local tool registry into provider requests and map them into Ollama `/api/chat` `tools`.

**Architecture:** Move shared tool metadata into `axercode-core`, update `ProviderRequest` to carry structured tool definitions, let `ToolRegistry` expose those definitions, update `ToolCallingAgent` to include them in every provider round, and map them into Ollama request JSON.

**Tech Stack:** Java 21, Spring Boot 3.3.4, Jackson, JUnit 5

---

### Task 1: Lock the shared tool-definition contract with failing tests

**Files:**
- Modify: `axercode-core/src/test/java/com/axercode/core/provider/ProviderContractsTest.java`
- Modify: `axercode-tools/src/test/java/com/axercode/tools/registry/ToolRegistryTest.java`

- [ ] **Step 1: Add a failing provider-contract test that expects structured tool definitions in `ProviderRequest`**
- [ ] **Step 2: Add a failing registry test that expects structured available tool definitions instead of only names**
- [ ] **Step 3: Run the targeted tests and confirm they fail for the expected missing-type or missing-method reasons**

### Task 2: Introduce shared tool metadata and wire it through the tool layer

**Files:**
- Create: `axercode-core/src/main/java/com/axercode/core/tool/ToolDefinition.java`
- Modify: `axercode-tools/src/main/java/com/axercode/tools/AxerTool.java`
- Modify: `axercode-tools/src/main/java/com/axercode/tools/builtin/ReadFileTool.java`
- Modify: `axercode-tools/src/main/java/com/axercode/tools/builtin/ListDirectoryTool.java`
- Modify: `axercode-tools/src/main/java/com/axercode/tools/builtin/RunShellTool.java`
- Modify: `axercode-tools/src/main/java/com/axercode/tools/registry/ToolRegistry.java`
- Delete: `axercode-tools/src/main/java/com/axercode/tools/ToolDefinition.java`

- [ ] **Step 1: Add the shared core `ToolDefinition`**
- [ ] **Step 2: Point `AxerTool` and all built-in tools at the shared definition**
- [ ] **Step 3: Add `ToolRegistry.availableTools()` returning structured definitions**
- [ ] **Step 4: Re-run targeted core/tools tests and confirm they pass**

### Task 3: Pass structured tool definitions into provider requests

**Files:**
- Modify: `axercode-core/src/main/java/com/axercode/core/provider/ProviderRequest.java`
- Modify: `axercode-agent/src/main/java/com/axercode/agent/ToolCallingAgent.java`
- Modify: `axercode-agent/src/test/java/com/axercode/agent/ToolCallingAgentTest.java`
- Modify: `axercode-cli/src/main/java/com/axercode/cli/config/ToolConfiguration.java`

- [ ] **Step 1: Change `ProviderRequest.availableTools` to use structured definitions**
- [ ] **Step 2: Update `ToolCallingAgent` to accept a `ToolRegistry` and include available tools in every provider request**
- [ ] **Step 3: Update CLI wiring and targeted agent tests**
- [ ] **Step 4: Re-run targeted agent tests and confirm they pass**

### Task 4: Map structured tool definitions into Ollama `/api/chat`

**Files:**
- Modify: `axercode-provider-ollama/src/main/java/com/axercode/provider/ollama/OllamaChatRequest.java`
- Modify: `axercode-provider-ollama/src/main/java/com/axercode/provider/ollama/OllamaChatProvider.java`
- Modify: `axercode-provider-ollama/src/test/java/com/axercode/provider/ollama/OllamaChatProviderTest.java`

- [ ] **Step 1: Add a failing Ollama provider test that expects `tools` in the serialized request**
- [ ] **Step 2: Remove the current “tool schema mapping is not implemented yet” guard**
- [ ] **Step 3: Serialize shared tool definitions into Ollama `tools`**
- [ ] **Step 4: Re-run targeted provider tests and confirm they pass**

### Task 5: Verify and document the step

**Files:**
- Create: `docs/steps/step-17-provider-tool-schema.md`

- [ ] **Step 1: Run `axercode-core` tests**
- [ ] **Step 2: Run `axercode-tools` tests**
- [ ] **Step 3: Run `axercode-agent` tests**
- [ ] **Step 4: Run `axercode-provider-ollama` tests**
- [ ] **Step 5: Run full reactor tests and package**
- [ ] **Step 6: Write the learning summary and Step 17 result block**

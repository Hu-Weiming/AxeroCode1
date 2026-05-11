# AxerCode Step 10 Tool Framework Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first reusable tool framework for AxerCode, including registration, execution dispatch, JSON argument parsing, and three local built-in tools.

**Architecture:** Keep the new framework isolated inside `axercode-tools` so later Agent and Provider steps can consume it without coupling local action execution to the CLI. Reuse `ToolCall` and `ToolExecutionResult` from `axercode-core`, and keep failure handling inside the tool layer rather than leaking exceptions to future callers.

**Tech Stack:** Java 21, Jackson Databind, JUnit 5

---

### Task 1: Add tool module dependencies and framework tests

**Files:**
- Modify: `axercode-tools/pom.xml`
- Create: `axercode-tools/src/test/java/com/axercode/tools/registry/ToolRegistryTest.java`
- Create: `axercode-tools/src/test/java/com/axercode/tools/execution/ToolExecutorTest.java`

- [ ] **Step 1: Add Jackson and JUnit dependencies to `axercode-tools`**
- [ ] **Step 2: Write a failing registry test for lookup and duplicate registration protection**
- [ ] **Step 3: Write a failing executor test for successful dispatch and unknown-tool failure**
- [ ] **Step 4: Run the targeted tests and verify they fail because the framework classes do not exist**
- [ ] **Step 5: Implement the minimal framework classes required for those tests**
- [ ] **Step 6: Re-run the targeted tests and verify they pass**

### Task 2: Add JSON argument parsing helper and `read_file`

**Files:**
- Create: `axercode-tools/src/main/java/com/axercode/tools/execution/ToolArguments.java`
- Create: `axercode-tools/src/main/java/com/axercode/tools/builtin/ReadFileTool.java`
- Create: `axercode-tools/src/test/java/com/axercode/tools/builtin/ReadFileToolTest.java`

- [ ] **Step 1: Write a failing test for reading file content from a JSON `path` argument**
- [ ] **Step 2: Write a failing test for missing `path` validation**
- [ ] **Step 3: Run the targeted tool tests and verify they fail**
- [ ] **Step 4: Implement the minimal JSON argument helper and `read_file` tool**
- [ ] **Step 5: Re-run the targeted tests and verify they pass**

### Task 3: Add `list_directory` and `run_shell`

**Files:**
- Create: `axercode-tools/src/main/java/com/axercode/tools/builtin/ListDirectoryTool.java`
- Create: `axercode-tools/src/main/java/com/axercode/tools/builtin/RunShellTool.java`
- Create: `axercode-tools/src/test/java/com/axercode/tools/builtin/ListDirectoryToolTest.java`
- Create: `axercode-tools/src/test/java/com/axercode/tools/builtin/RunShellToolTest.java`

- [ ] **Step 1: Write a failing test for listing directory entries**
- [ ] **Step 2: Write a failing test for recursive listing**
- [ ] **Step 3: Write a failing test for shell command execution with output capture**
- [ ] **Step 4: Write a failing test for shell timeout or invalid-argument handling**
- [ ] **Step 5: Run the targeted tests and verify they fail**
- [ ] **Step 6: Implement the minimal `list_directory` and `run_shell` tools**
- [ ] **Step 7: Re-run the targeted tests and verify they pass**

### Task 4: Verify the module and document the step

**Files:**
- Create: `docs/steps/step-10-tool-framework.md`

- [ ] **Step 1: Run `axercode-tools` tests**
- [ ] **Step 2: Run the full reactor tests**
- [ ] **Step 3: Package the project**
- [ ] **Step 4: Write the learning summary and Step 10 result block**

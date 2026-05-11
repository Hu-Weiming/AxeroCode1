# AxerCode Step 8 JLine Shell Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the current interactive CLI shell to a JLine-powered terminal experience with line editing, persisted history, and slash-command completion while preserving the Step 7 session flow and one-shot prompt mode.

**Architecture:** Keep `CliChatService` and `InMemorySessionStore` as the session and provider boundary, and replace only the interactive input layer with a `JLine` terminal and `LineReader`. `AxerCodeCliCommand` should dispatch `--interactive` through the new JLine entrypoint, while tests keep a plain Reader-based path available for deterministic shell behavior checks.

**Tech Stack:** Java 21, Spring Boot 3.3.4, Picocli 4.7.7, JLine 3.30.6, JUnit 5

---

### Task 1: Add the JLine dependency and shell configuration surface

**Files:**
- Modify: `pom.xml`
- Modify: `axercode-cli/pom.xml`
- Create: `axercode-cli/src/main/java/com/axercode/cli/config/AxerCodeShellProperties.java`
- Modify: `axercode-cli/src/main/java/com/axercode/cli/bootstrap/AxerCodeCliApplication.java`
- Modify: `axercode-cli/src/main/resources/application.yml`

- [ ] **Step 1: Add the shared JLine version and CLI dependency**
- [ ] **Step 2: Add shell configuration properties for the history file path**
- [ ] **Step 3: Register the new properties with the Spring Boot CLI application**
- [ ] **Step 4: Set the default history path in `application.yml`**

### Task 2: Drive the JLine shell behavior with tests

**Files:**
- Modify: `axercode-cli/src/test/java/com/axercode/cli/service/InteractiveShellServiceTest.java`

- [ ] **Step 1: Add a failing test for terminal-backed interactive shell flow**
- [ ] **Step 2: Add a failing test for JLine history file persistence**
- [ ] **Step 3: Run the targeted shell tests and verify the failures are for the missing JLine behavior**
- [ ] **Step 4: Keep the Reader/Writer tests as regression coverage for shell commands and multi-turn state**

### Task 3: Finish the JLine shell implementation

**Files:**
- Modify: `axercode-cli/src/main/java/com/axercode/cli/service/InteractiveShellService.java`
- Modify: `axercode-cli/src/main/java/com/axercode/cli/command/AxerCodeCliCommand.java`
- Modify: `axercode-cli/src/test/java/com/axercode/cli/command/AxerCodeCliCommandTest.java`

- [ ] **Step 1: Build the JLine `Terminal` and `LineReader` path inside `InteractiveShellService`**
- [ ] **Step 2: Persist shell history and expose slash-command completion**
- [ ] **Step 3: Switch the Picocli interactive path to `runInteractive(...)`**
- [ ] **Step 4: Re-run the targeted tests and verify they pass**

### Task 4: Verify the packaged CLI and document the step

**Files:**
- Create: `docs/steps/step-8-jline-shell.md`

- [ ] **Step 1: Run the `axercode-cli` module tests**
- [ ] **Step 2: Run the full reactor tests**
- [ ] **Step 3: Package the CLI jar**
- [ ] **Step 4: Run the CLI jar in interactive mode with scripted input and verify JLine-backed shell behavior**
- [ ] **Step 5: Write the learning summary and Step 8 result block**

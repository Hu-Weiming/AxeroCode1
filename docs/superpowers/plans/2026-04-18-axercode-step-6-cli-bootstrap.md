# AxerCode Step 6 CLI Bootstrap Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn `axercode-cli` into the first executable Spring Boot 3.x console shell that can send a single prompt to the Ollama provider and print the result.

**Architecture:** This step introduces a thin Spring Boot 3.x console application with Picocli as the argument layer. The CLI shell will delegate prompt handling to a small chat service that builds `ProviderRequest` objects and uses the shared `LlmProvider` abstraction, while a provider configuration class wires in the current `OllamaChatProvider`.

**Tech Stack:** Java 21, Spring Boot 3.3.4, Picocli 4.7.x, JUnit 5, Spring Boot Test, Maven Surefire

---

### Task 1: Prepare the CLI module dependencies

**Files:**
- Modify: `pom.xml`
- Modify: `axercode-cli/pom.xml`

- [ ] **Step 1: Add a shared picocli version property in the parent POM**
- [ ] **Step 2: Add Spring Boot 3.x, picocli, provider-api, and test dependencies to `axercode-cli`**
- [ ] **Step 3: Verify the reactor still validates**

### Task 2: Add chat service and command tests first

**Files:**
- Create: `axercode-cli/src/test/java/com/axercode/cli/service/CliChatServiceTest.java`
- Create: `axercode-cli/src/test/java/com/axercode/cli/command/AxerCodeCliCommandTest.java`

- [ ] **Step 1: Write failing tests for prompt request construction, tool-call fallback output, successful command output, and provider error handling**
- [ ] **Step 2: Run the targeted CLI tests and verify they fail because the CLI classes do not exist**

### Task 3: Implement the minimal Spring Boot 3.x CLI shell

**Files:**
- Create: `axercode-cli/src/main/java/com/axercode/cli/bootstrap/AxerCodeCliApplication.java`
- Create: `axercode-cli/src/main/java/com/axercode/cli/bootstrap/PicocliRunner.java`
- Create: `axercode-cli/src/main/java/com/axercode/cli/config/AxerCodeProviderProperties.java`
- Create: `axercode-cli/src/main/java/com/axercode/cli/config/ProviderConfiguration.java`
- Create: `axercode-cli/src/main/java/com/axercode/cli/service/CliChatService.java`
- Create: `axercode-cli/src/main/java/com/axercode/cli/command/AxerCodeCliCommand.java`
- Create: `axercode-cli/src/main/resources/application.yml`

- [ ] **Step 1: Write the minimal production code to bootstrap Spring Boot, wire the provider bean, and execute a single `--prompt` command**
- [ ] **Step 2: Re-run the targeted CLI tests and verify they pass**

### Task 4: Verify the CLI path and document the step

**Files:**
- Create: `docs/steps/step-6-cli-bootstrap.md`

- [ ] **Step 1: Run `axercode-cli` tests**
- [ ] **Step 2: Run the full reactor test phase**
- [ ] **Step 3: Run a real `spring-boot:run` one-shot prompt against local Ollama**
- [ ] **Step 4: Write the learning summary**

# AxerCode Step 5 Provider Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn `axercode-provider-api` and `axercode-provider-ollama` into the first compileable provider layer with a unified provider interface and a minimal Ollama `/api/chat` adapter.

**Architecture:** This step keeps the provider boundary thin and explicit. `axercode-provider-api` defines the stable abstraction that later Spring Boot 3.x CLI and server modules will depend on, while `axercode-provider-ollama` implements a small local adapter that converts the shared core contracts into the Ollama chat payload and maps the response back into `ProviderResponse`.

**Tech Stack:** Java 21, Maven 3.9.12, JUnit 5, Spring Web 6.x via Spring Boot 3.3.4 BOM, Jackson, JDK HttpServer

---

### Task 1: Add provider API build and contract tests

**Files:**
- Modify: `axercode-provider-api/pom.xml`
- Create: `axercode-provider-api/src/test/java/com/axercode/provider/api/LlmProviderContractTest.java`
- Create: `axercode-provider-api/src/test/java/com/axercode/provider/api/ProviderExceptionTest.java`
- Create: `axercode-provider-api/src/main/java/com/axercode/provider/api/LlmProvider.java`
- Create: `axercode-provider-api/src/main/java/com/axercode/provider/api/ProviderException.java`

- [ ] **Step 1: Write the failing provider API tests**
- [ ] **Step 2: Run the targeted tests and verify they fail because the API types do not exist**
- [ ] **Step 3: Write the minimal provider API production code**
- [ ] **Step 4: Re-run the targeted tests and verify they pass**

### Task 2: Add Ollama adapter dependencies and request/response tests

**Files:**
- Modify: `axercode-provider-ollama/pom.xml`
- Create: `axercode-provider-ollama/src/test/java/com/axercode/provider/ollama/OllamaChatProviderTest.java`

- [ ] **Step 1: Write the failing Ollama provider tests for text response, tool-call response, and HTTP error wrapping**
- [ ] **Step 2: Run the targeted tests and verify they fail because the Ollama provider does not exist**

### Task 3: Implement the minimal Ollama `/api/chat` adapter

**Files:**
- Create: `axercode-provider-ollama/src/main/java/com/axercode/provider/ollama/OllamaChatProvider.java`
- Create: `axercode-provider-ollama/src/main/java/com/axercode/provider/ollama/OllamaChatRequest.java`
- Create: `axercode-provider-ollama/src/main/java/com/axercode/provider/ollama/OllamaChatResponse.java`

- [ ] **Step 1: Write the minimal Ollama adapter production code**
- [ ] **Step 2: Re-run the targeted tests and verify they pass**

### Task 4: Verify the modules and document the step

**Files:**
- Create: `docs/steps/step-5-provider-foundation.md`

- [ ] **Step 1: Run `axercode-provider-api` tests**
- [ ] **Step 2: Run `axercode-provider-ollama` tests**
- [ ] **Step 3: Run the full reactor test phase**
- [ ] **Step 4: Write the learning summary**

# Step 18 CLI Streaming Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add token streaming to the AxerCode CLI while preserving the existing synchronous fallback path.

**Architecture:** Introduce an optional provider streaming contract, implement it in the Ollama adapter using NDJSON `/api/chat`, then thread a streaming callback through the agent and CLI layers. Keep the old synchronous path intact for unsupported providers and failure fallback.

**Tech Stack:** Java 21, Spring Boot 3.3.4, RestClient, Jackson, JUnit 5, Picocli, JLine 3, Ollama `/api/chat`

---

### Task 1: Define the provider streaming contract

**Files:**
- Modify: `D:\AeroCode1\axercode-provider-api\src\main\java\com\axercode\provider\api\LlmProvider.java`
- Create: `D:\AeroCode1\axercode-provider-api\src\test\java\com\axercode\provider\api\LlmProviderStreamingContractTest.java`

- [ ] Add a failing contract test for the default streaming behavior.
- [ ] Run the provider-api test to verify the red state.
- [ ] Add the minimal `streamGenerate(...)` method to `LlmProvider`.
- [ ] Re-run provider-api tests to verify green.

### Task 2: Implement Ollama NDJSON streaming

**Files:**
- Modify: `D:\AeroCode1\axercode-provider-ollama\src\main\java\com\axercode\provider\ollama\OllamaChatProvider.java`
- Modify: `D:\AeroCode1\axercode-provider-ollama\src\main\java\com\axercode\provider\ollama\OllamaChatResponse.java`
- Modify: `D:\AeroCode1\axercode-provider-ollama\src\test\java\com\axercode\provider\ollama\OllamaChatProviderTest.java`

- [ ] Add failing tests for streamed text completion and streamed tool-call completion.
- [ ] Run the provider-ollama tests to verify the red state.
- [ ] Implement NDJSON line reading and chunk mapping in `OllamaChatProvider`.
- [ ] Re-run provider-ollama tests to verify green.

### Task 3: Add agent streaming support

**Files:**
- Modify: `D:\AeroCode1\axercode-agent\src\main\java\com\axercode\agent\ConversationAgent.java`
- Modify: `D:\AeroCode1\axercode-agent\src\main\java\com\axercode\agent\ToolCallingAgent.java`
- Modify: `D:\AeroCode1\axercode-agent\src\test\java\com\axercode\agent\ToolCallingAgentTest.java`

- [ ] Add failing tests covering streamed completion and streamed tool loop behavior.
- [ ] Run the agent tests to verify the red state.
- [ ] Implement the minimal streaming method in `ToolCallingAgent`.
- [ ] Re-run agent tests to verify green.

### Task 4: Surface streaming in the CLI

**Files:**
- Modify: `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\CliChatService.java`
- Modify: `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\InteractiveShellService.java`
- Modify: `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\command\AxerCodeCliCommand.java`
- Modify: `D:\AeroCode1\axercode-cli\src\test\java\com\axercode\cli\service\InteractiveShellServiceTest.java`
- Modify: `D:\AeroCode1\axercode-cli\src\test\java\com\axercode\cli\command\AxerCodeCliCommandTest.java`

- [ ] Add failing tests proving streamed output in one-shot and interactive modes.
- [ ] Run the CLI tests to verify the red state.
- [ ] Implement streaming callbacks with synchronous fallback.
- [ ] Re-run CLI tests to verify green.

### Task 5: Verify and document Step 18

**Files:**
- Create: `D:\AeroCode1\docs\steps\step-18-cli-streaming.md`

- [ ] Run `scripts\mvn-jdk21.cmd -q -pl axercode-provider-api -am test`.
- [ ] Run `scripts\mvn-jdk21.cmd -q -pl axercode-provider-ollama -am test`.
- [ ] Run `scripts\mvn-jdk21.cmd -q -pl axercode-agent -am test`.
- [ ] Run `scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test`.
- [ ] Run `scripts\mvn-jdk21.cmd -q test`.
- [ ] Run `scripts\mvn-jdk21.cmd -q package`.
- [ ] Run the packaged CLI on JDK 21 and verify streamed output appears progressively.
- [ ] Write the Step 18 learning summary.

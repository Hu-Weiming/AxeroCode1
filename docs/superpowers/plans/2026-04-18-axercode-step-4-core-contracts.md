# AxerCode Step 4 Core Contracts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expand `axercode-core` with the provider-facing, tool-result, and session-context contracts that later CLI, Ollama, and agent modules will share.

**Architecture:** This step still keeps all production code inside `axercode-core` so the domain model can grow without infrastructure coupling. The new types are immutable Java 21 records and enums with narrow factory methods, letting later Spring Boot 3.x modules depend on stable contracts instead of inventing transport-specific shapes.

**Tech Stack:** Java 21, Maven 3.9.12, JUnit 5, Maven Surefire, immutable records

---

### Task 1: Add session identity and conversation context with TDD

**Files:**
- Create: `axercode-core/src/test/java/com/axercode/core/session/SessionContextTest.java`
- Create: `axercode-core/src/main/java/com/axercode/core/session/SessionId.java`
- Create: `axercode-core/src/main/java/com/axercode/core/session/SessionContext.java`

- [ ] **Step 1: Write the failing session tests**

```java
@Test
void startCreatesEmptyContextWithGeneratedSessionId() {
    SessionContext context = SessionContext.start();
    assertNotNull(context.sessionId());
    assertEquals(0, context.messages().size());
}
```

- [ ] **Step 2: Run the targeted test and verify it fails**

Run: `scripts\mvn-jdk21.cmd -q -pl axercode-core test "-Dtest=SessionContextTest"`
Expected: compilation failure because `SessionContext` and `SessionId` do not exist yet

- [ ] **Step 3: Write the minimal implementation**

```java
public record SessionContext(SessionId sessionId, List<ConversationMessage> messages) {
    public static SessionContext start() {
        return new SessionContext(SessionId.create(), List.of());
    }
}
```

- [ ] **Step 4: Re-run the targeted test**

Run: `scripts\mvn-jdk21.cmd -q -pl axercode-core test "-Dtest=SessionContextTest"`
Expected: PASS

### Task 2: Add tool execution result contracts with TDD

**Files:**
- Create: `axercode-core/src/test/java/com/axercode/core/tool/ToolExecutionResultTest.java`
- Create: `axercode-core/src/main/java/com/axercode/core/tool/ToolExecutionStatus.java`
- Create: `axercode-core/src/main/java/com/axercode/core/tool/ToolExecutionResult.java`

- [ ] **Step 1: Write the failing tool result tests**

```java
@Test
void successFactoryCapturesToolMetadata() {
    ToolCall call = ToolCall.create("read_file", "{\"path\":\"README.md\"}");
    ToolExecutionResult result = ToolExecutionResult.success(call, "file contents");
    assertEquals(ToolExecutionStatus.SUCCESS, result.status());
}
```

- [ ] **Step 2: Run the targeted test and verify it fails**

Run: `scripts\mvn-jdk21.cmd -q -pl axercode-core test "-Dtest=ToolExecutionResultTest"`
Expected: compilation failure because `ToolExecutionResult` and `ToolExecutionStatus` do not exist yet

- [ ] **Step 3: Write the minimal implementation**

```java
public record ToolExecutionResult(ToolCall toolCall, ToolExecutionStatus status, String output, Instant createdAt) {
    public static ToolExecutionResult success(ToolCall toolCall, String output) {
        return new ToolExecutionResult(toolCall, ToolExecutionStatus.SUCCESS, output, Instant.now());
    }
}
```

- [ ] **Step 4: Re-run the targeted test**

Run: `scripts\mvn-jdk21.cmd -q -pl axercode-core test "-Dtest=ToolExecutionResultTest"`
Expected: PASS

### Task 3: Add provider request and response contracts with TDD

**Files:**
- Create: `axercode-core/src/test/java/com/axercode/core/provider/ProviderContractsTest.java`
- Create: `axercode-core/src/main/java/com/axercode/core/provider/ProviderStopReason.java`
- Create: `axercode-core/src/main/java/com/axercode/core/provider/ProviderRequest.java`
- Create: `axercode-core/src/main/java/com/axercode/core/provider/ProviderResponse.java`

- [ ] **Step 1: Write the failing provider contract tests**

```java
@Test
void requestCopiesMessagesAndAvailableToolsDefensively() {
    ProviderRequest request = ProviderRequest.create("qwen2.5:7b", messages, tools, true);
    assertEquals(1, request.messages().size());
}
```

- [ ] **Step 2: Run the targeted test and verify it fails**

Run: `scripts\mvn-jdk21.cmd -q -pl axercode-core test "-Dtest=ProviderContractsTest"`
Expected: compilation failure because the provider contracts do not exist yet

- [ ] **Step 3: Write the minimal implementation**

```java
public record ProviderResponse(String content, List<ToolCall> toolCalls, ProviderStopReason stopReason) {
    public static ProviderResponse complete(String content) {
        return new ProviderResponse(content, List.of(), ProviderStopReason.COMPLETE);
    }
}
```

- [ ] **Step 4: Re-run the targeted test**

Run: `scripts\mvn-jdk21.cmd -q -pl axercode-core test "-Dtest=ProviderContractsTest"`
Expected: PASS

### Task 4: Verify the module and write the step summary

**Files:**
- Create: `docs/steps/step-4-core-contracts.md`

- [ ] **Step 1: Run the core module tests**

Run: `scripts\mvn-jdk21.cmd -q -pl axercode-core test`
Expected: PASS

- [ ] **Step 2: Run the full reactor tests**

Run: `scripts\mvn-jdk21.cmd -q test`
Expected: PASS

- [ ] **Step 3: Write the learning summary**

```md
## Step Goal

Add the next layer of reusable core contracts for session tracking, provider integration, and tool observations.
```

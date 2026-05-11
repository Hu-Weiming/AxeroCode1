# AxerCode Step 3 Core Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn `axercode-core` into the first compileable Java module with foundational domain types for messages, tool calls, and agent requests.

**Architecture:** This step keeps all production code inside `axercode-core` so the first source layer stays isolated and easy to verify. Tests define the initial API first, then minimal immutable Java 21 records implement those contracts without pulling in Spring or infrastructure concerns.

**Tech Stack:** Java 21, Maven 3.9.12, JUnit 5, Maven Surefire, Maven Compiler Plugin

---

### Task 1: Prepare the parent build for test execution

**Files:**
- Modify: `pom.xml`
- Modify: `axercode-core/pom.xml`

- [ ] **Step 1: Add a modern Surefire plugin to the parent build**

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.5.0</version>
</plugin>
```

- [ ] **Step 2: Add JUnit Jupiter test support to `axercode-core`**

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 3: Verify Maven still validates after the build changes**

Run: `scripts\mvn-jdk21.cmd -q validate`
Expected: exit code `0` and no reactor errors

### Task 2: Define the first core message model with TDD

**Files:**
- Create: `axercode-core/src/test/java/com/axercode/core/session/ConversationMessageTest.java`
- Create: `axercode-core/src/main/java/com/axercode/core/session/MessageRole.java`
- Create: `axercode-core/src/main/java/com/axercode/core/session/ConversationMessage.java`

- [ ] **Step 1: Write the failing message model tests**

```java
@Test
void userFactoryCreatesUserMessageWithGeneratedMetadata() {
    ConversationMessage message = ConversationMessage.user("Explain the last diff");

    assertEquals(MessageRole.USER, message.role());
    assertEquals("Explain the last diff", message.content());
    assertNotNull(message.id());
    assertNotNull(message.createdAt());
}
```

- [ ] **Step 2: Run the targeted test and watch it fail**

Run: `scripts\mvn-jdk21.cmd -q -pl axercode-core test -Dtest=ConversationMessageTest`
Expected: compilation failure because `ConversationMessage` and `MessageRole` do not exist yet

- [ ] **Step 3: Write the minimal production code**

```java
public record ConversationMessage(UUID id, MessageRole role, String content, Instant createdAt) {
    public ConversationMessage {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
    }
}
```

- [ ] **Step 4: Re-run the targeted test**

Run: `scripts\mvn-jdk21.cmd -q -pl axercode-core test -Dtest=ConversationMessageTest`
Expected: PASS

### Task 3: Define tool call and agent request types with TDD

**Files:**
- Create: `axercode-core/src/test/java/com/axercode/core/tool/ToolCallTest.java`
- Create: `axercode-core/src/test/java/com/axercode/core/agent/AgentRequestTest.java`
- Create: `axercode-core/src/main/java/com/axercode/core/tool/ToolCall.java`
- Create: `axercode-core/src/main/java/com/axercode/core/agent/AgentRequest.java`

- [ ] **Step 1: Write the failing tool call and agent request tests**

```java
@Test
void createRejectsBlankToolName() {
    assertThrows(IllegalArgumentException.class, () -> ToolCall.create(" ", "{}"));
}

@Test
void createCopiesMessagesDefensively() {
    List<ConversationMessage> messages = new ArrayList<>();
    messages.add(ConversationMessage.user("Open README"));
    AgentRequest request = AgentRequest.create(messages);
    messages.clear();
    assertEquals(1, request.messages().size());
}
```

- [ ] **Step 2: Run the targeted tests and watch them fail**

Run: `scripts\mvn-jdk21.cmd -q -pl axercode-core test -Dtest=ToolCallTest,AgentRequestTest`
Expected: compilation failure because `ToolCall` and `AgentRequest` do not exist yet

- [ ] **Step 3: Write the minimal production code**

```java
public record ToolCall(UUID id, String name, String argumentsJson) {
    public static ToolCall create(String name, String argumentsJson) {
        return new ToolCall(UUID.randomUUID(), name, argumentsJson);
    }
}
```

```java
public record AgentRequest(List<ConversationMessage> messages) {
    public static AgentRequest create(List<ConversationMessage> messages) {
        return new AgentRequest(List.copyOf(messages));
    }
}
```

- [ ] **Step 4: Re-run the targeted tests**

Run: `scripts\mvn-jdk21.cmd -q -pl axercode-core test -Dtest=ToolCallTest,AgentRequestTest`
Expected: PASS

### Task 4: Verify the module and document the step

**Files:**
- Create: `docs/steps/step-3-core-foundation.md`

- [ ] **Step 1: Run the module test suite**

Run: `scripts\mvn-jdk21.cmd -q -pl axercode-core test`
Expected: PASS

- [ ] **Step 2: Run the full reactor test phase**

Run: `scripts\mvn-jdk21.cmd -q test`
Expected: PASS with the empty modules still succeeding

- [ ] **Step 3: Write the learning summary**

```md
## Step Goal

Create the first compileable Java source module for AxerCode.
```

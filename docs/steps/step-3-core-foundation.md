# Step 3 - First Compileable Core Module

## Step Goal

Create the first real Java source code in the AxerCode repository by turning `axercode-core` into a compileable, test-backed module.

## What Was Done

1. Added JUnit 5 test support to `axercode-core`.
2. Added Maven Surefire to the parent build so tests run consistently across modules.
3. Created the first `axercode-core` package structure under `com.axercode.core`.
4. Introduced three foundational domain types:
   - `ConversationMessage`
   - `ToolCall`
   - `AgentRequest`
5. Added focused tests for each type and implemented them through a TDD red-green cycle.

## Files Added or Changed

### Build Files

- `pom.xml`
- `axercode-core/pom.xml`

### Production Code

- `axercode-core/src/main/java/com/axercode/core/session/MessageRole.java`
- `axercode-core/src/main/java/com/axercode/core/session/ConversationMessage.java`
- `axercode-core/src/main/java/com/axercode/core/tool/ToolCall.java`
- `axercode-core/src/main/java/com/axercode/core/agent/AgentRequest.java`

### Tests

- `axercode-core/src/test/java/com/axercode/core/session/ConversationMessageTest.java`
- `axercode-core/src/test/java/com/axercode/core/tool/ToolCallTest.java`
- `axercode-core/src/test/java/com/axercode/core/agent/AgentRequestTest.java`

## How It Was Implemented

## 1. Test Infrastructure First

Before writing production code, the build needed a modern test runner. The parent build now declares Maven Surefire `3.5.0`, and `axercode-core` now depends on JUnit Jupiter.

That gives the repository a clean place to start test-first development without adding Spring or heavier dependencies too early.

## 2. Conversation Message Model

`ConversationMessage` is the smallest useful chat primitive in this project. It holds:

- a generated message id
- a `MessageRole`
- non-blank content
- a creation timestamp

The static `user(...)` factory exists because later CLI and Web layers will constantly need to transform raw user input into a normalized internal message type.

## 3. Tool Call Model

`ToolCall` represents the agent deciding to invoke a named tool. Even though the tool system is not implemented yet, this type lets future provider adapters and the ReAct loop share one stable contract.

The implementation intentionally normalizes blank JSON arguments to `{}` so callers do not have to special-case missing payloads.

## 4. Agent Request Model

`AgentRequest` is the minimum boundary object between delivery layers and the future agent engine.

Right now it only carries messages, but it already enforces two important rules:

- the caller must provide at least one message
- the internal list is defensively copied so outside mutation cannot corrupt agent input

## TDD Evidence

This step followed a real red-green cycle:

1. `ConversationMessageTest` was written first and failed because `ConversationMessage` and `MessageRole` did not exist.
2. Minimal production code was added and the targeted test was re-run successfully.
3. `ToolCallTest` and `AgentRequestTest` were written next and failed because those types did not exist.
4. Minimal production code was added and the targeted tests were re-run successfully.

## Verification Commands

The following commands were executed successfully in `D:\AeroCode1`:

```bat
scripts\mvn-jdk21.cmd -q validate
scripts\mvn-jdk21.cmd -q -pl axercode-core test -Dtest=ConversationMessageTest
scripts\mvn-jdk21.cmd -q -pl axercode-core test "-Dtest=ToolCallTest,AgentRequestTest"
scripts\mvn-jdk21.cmd -q -pl axercode-core test
scripts\mvn-jdk21.cmd -q test
```

## Why This Step Matters

This is the first point where AxerCode stops being only a build skeleton and starts having an internal language.

Without these kinds of core types, later modules would be forced to invent their own ad-hoc request objects, which usually leads to coupling and duplicate logic.

## Next Step

Step 4 should keep building out `axercode-core` before any provider or CLI code is written:

- add provider-facing request and response contracts
- add tool result or observation models
- add basic session identifiers and context containers

# Step 11 - Tool Calling Agent

## Step Goal

Add the first real tool-aware agent path so provider-returned `tool_calls` are executed locally and fed back into the conversation before producing a final reply.

## What Was Done

1. Added a lightweight agent boundary in `axercode-agent`.
2. Implemented `ToolCallingAgent` with one round of tool execution.
3. Moved CLI turn orchestration out of `CliChatService` and into the agent layer.
4. Added CLI bean wiring for built-in tools, `ToolRegistry`, `ToolExecutor`, and the new `ConversationAgent`.
5. Verified the new agent path with dedicated tests for both plain replies and tool-call loops.

## Files Added or Changed

### Production Code

- `axercode-agent/pom.xml`
- `axercode-agent/src/main/java/com/axercode/agent/ConversationAgent.java`
- `axercode-agent/src/main/java/com/axercode/agent/AgentConversationTurn.java`
- `axercode-agent/src/main/java/com/axercode/agent/ToolCallingAgent.java`
- `axercode-cli/src/main/java/com/axercode/cli/service/CliChatService.java`
- `axercode-cli/src/main/java/com/axercode/cli/config/ToolConfiguration.java`

### Tests

- `axercode-agent/src/test/java/com/axercode/agent/ToolCallingAgentTest.java`
- `axercode-cli/src/test/java/com/axercode/cli/service/CliChatServiceTest.java`
- `axercode-cli/src/test/java/com/axercode/cli/service/InteractiveShellServiceTest.java`

## How It Was Implemented

## 1. Lightweight Agent Boundary

`axercode-agent` now contains:

- `ConversationAgent`
- `AgentConversationTurn`
- `ToolCallingAgent`

This creates a real orchestration boundary between the CLI shell and the provider/tool stack.

The CLI no longer needs to know how many provider calls happen inside one turn.

## 2. One Tool-Execution Round

`ToolCallingAgent` runs this sequence:

1. append the user prompt to the current `SessionContext`
2. call the provider once
3. if the provider completes normally, append the assistant reply and return
4. if the provider returns `tool_calls`, execute them through `ToolExecutor`
5. append each tool observation as a `TOOL` message
6. call the provider a second time for the final assistant reply

This is intentionally limited to one tool-execution round in Step 11.

If the second provider response still returns `tool_calls`, the agent returns a friendly placeholder:

`[AxerCode] Nested tool calls are not implemented yet.`

That keeps behavior honest and prevents pretending a full ReAct loop already exists.

## 3. Tool Observation Format

Tool results are converted into `TOOL` messages with a stable textual shape:

`TOOL <name> [<status>]`

followed by the tool output body.

That is simple enough for current provider contracts and stable enough for later prompt and provider refinements.

## 4. CLI Integration

`CliChatService` now has a smaller job:

- validate prompt and session input
- choose the effective model
- delegate the turn to `ConversationAgent`
- convert the result into `CliChatTurn`

This is an important boundary cleanup.

The CLI shell remains focused on interaction, while the agent owns orchestration.

## 5. Spring Wiring

`ToolConfiguration` in the CLI module now creates beans for:

- `ReadFileTool`
- `ListDirectoryTool`
- `RunShellTool`
- `ToolRegistry`
- `ToolExecutor`
- `ConversationAgent`

This keeps `axercode-agent` plain Java and reusable, while the CLI module remains the Spring Boot composition root.

## Why This Design

The main design choice was to stop at a first real agent loop instead of skipping straight to a full ReAct engine.

That gives AxerCode a meaningful new capability without collapsing multiple future steps into one:

- provider call orchestration is now separated from the CLI
- tool execution is now part of a real agent path
- the loop is still simple enough to test thoroughly

## Important Limitation

Step 11 does **not** mean real local Ollama tool selection is already fully enabled.

The current Ollama provider still does not send tool schema metadata to the model, so real model-driven tool choice in local runtime is a later step.

What Step 11 does guarantee is:

- if the provider returns `tool_calls`
- the agent can now execute them
- and complete a second provider round with tool observations

## TDD Evidence

This step followed red-green loops in two layers:

1. `ToolCallingAgentTest` failed first because the agent types and loop did not exist.
2. `CliChatServiceTest` failed first because the CLI still depended on the provider instead of the agent.
3. `InteractiveShellServiceTest` then had to be updated to the new agent-driven boundary.

Minimal production code was added until the agent tests and CLI tests all passed again.

## Verification Commands

The following commands were executed successfully in `D:\AeroCode1`:

```bat
scripts\mvn-jdk21.cmd -q -pl axercode-agent -am test "-Dtest=ToolCallingAgentTest" "-Dsurefire.failIfNoSpecifiedTests=false"
scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test "-Dtest=CliChatServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"
scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test "-Dtest=InteractiveShellServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"
scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test "-Dtest=AxerCodeCliCommandTest" "-Dsurefire.failIfNoSpecifiedTests=false"
scripts\mvn-jdk21.cmd -q -pl axercode-agent -am test
scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test
scripts\mvn-jdk21.cmd -q test
scripts\mvn-jdk21.cmd -q package
```

## Why This Step Matters

This is the first point where AxerCode has a real agent execution path instead of a shell that talks directly to the model.

It now supports:

- agent-owned conversation turns
- provider `tool_calls` becoming real local actions
- `TOOL` observation messages in session history
- a second provider request after tool execution
- clean CLI-to-agent separation

## Next Step

Step 12 should extend this single-round tool loop into a more complete iterative agent loop with clearer stopping conditions and better error recovery.

# AxerCode Tool Calling Agent Design

## Goal

Build the first real Agent execution path for AxerCode so provider-returned `tool_calls` become actual local actions instead of placeholder text.

## Scope

This step covers:

- a lightweight agent boundary in `axercode-agent`
- one round of tool execution after provider `tool_calls`
- conversion of tool results into `TOOL` conversation messages
- a second provider request for the final assistant reply
- CLI integration through `CliChatService`

This step does **not** cover:

- provider-side tool schema mapping for real Ollama tool selection
- multi-step ReAct loops
- reflection or retry logic
- Web integration

## Approaches Considered

### 1. Put tool execution directly in `CliChatService`

Fastest path, but it would couple agent behavior to one UI shell.

### 2. Add a lightweight `ToolCallingAgent` in `axercode-agent`

Lets the CLI delegate agent behavior while keeping the loop logic reusable.

### 3. Skip to a full multi-step ReAct loop now

Powerful, but too large for this step and too likely to blur responsibilities.

## Recommended Design

Use approach 2.

Add:

- `ConversationAgent` interface
- `AgentConversationTurn` result model
- `ToolCallingAgent` implementation

Execution shape:

1. append the user message to the current session
2. call the provider once
3. if the provider completes normally, return the assistant reply
4. if the provider returns `tool_calls`, execute each tool through `ToolExecutor`
5. append tool observations as `TOOL` messages
6. call the provider a second time for the final assistant reply

If the second provider response still contains `tool_calls`, return a clear placeholder rather than pretending nested loops are implemented.

## CLI Integration

`CliChatService` should stop talking directly to `LlmProvider`.

Instead it should:

- choose the effective model
- validate prompt and session
- delegate the full turn to `ConversationAgent`
- convert the agent result into `CliChatTurn`

## Spring Wiring

Keep `axercode-agent` plain Java.

Create CLI-side bean configuration for:

- built-in tools
- `ToolRegistry`
- `ToolExecutor`
- `ConversationAgent`

That keeps `axercode-agent` reusable and avoids pulling Spring annotations into the module itself.

## Testing Strategy

- agent tests for plain provider completion
- agent tests for `tool_calls -> tool execution -> second provider request`
- CLI tests for model selection and delegation to the agent

## Outcome

After this step, AxerCode will have its first real tool-aware agent path, even though real provider-side tool advertisement to Ollama is still a later step.

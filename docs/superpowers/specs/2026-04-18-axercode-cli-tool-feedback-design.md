# AxerCode CLI Tool Feedback Design

## Goal

Expose the agent's executed tool results in the CLI so users can see what happened during a turn instead of only seeing the final assistant reply.

## Scope

This step covers:

- surfacing executed `ToolExecutionResult` items from the agent layer into the CLI layer
- formatting tool execution output for human-readable console display
- showing tool feedback in both one-shot `--prompt` mode and interactive shell mode

This step does **not** cover:

- changing the provider contract
- adding real-time token streaming
- adding real-time per-tool progress callbacks
- adding plan mode or advanced slash commands

## Problem

`ToolCallingAgent` already executes tools and returns `toolResults`, but the CLI currently discards that information.

That creates two issues:

1. users cannot tell whether the agent used tools or not
2. debugging tool behavior from the shell is much harder than it needs to be

## Approaches Considered

### 1. Print raw tool outputs directly inside the shell

Fastest to implement, but it spreads formatting logic across CLI entry points and makes future refinement awkward.

### 2. Add a dedicated CLI formatter for tool feedback

Keeps formatting logic isolated and lets both one-shot and interactive paths share the same output shape.

### 3. Add real-time event streaming from agent to shell

Most powerful long term, but too broad for this step because it changes the agent-to-CLI boundary and starts overlapping with later streaming work.

## Recommended Design

Use approach 2.

The step should:

1. keep `AgentConversationTurn` unchanged
2. extend `CliChatTurn` so it carries `toolResults`
3. add a CLI-side formatter that turns tool results into stable printable blocks
4. update the one-shot and interactive shell paths to print tool feedback before the final assistant reply

## Output Shape

Tool feedback should be concise and readable, for example:

```text
[tool] read_file SUCCESS
<tool output>
```

If multiple tools run in one turn, print each block in execution order.

This keeps the CLI honest without requiring a streaming protocol yet.

## Why This Design

The main goal here is visibility, not new orchestration behavior.

The agent already knows what happened. The CLI just needs to stop throwing that information away.

By introducing a small formatter boundary now, later steps can improve the rendering style without touching the agent or shell control flow again.

## Testing Strategy

Add tests for:

- `CliChatService` preserving tool results from the agent
- one-shot CLI printing tool feedback before the final reply
- interactive shell printing tool feedback during a prompt turn
- empty tool result lists producing no extra tool output

## Outcome

After this step, AxerCode users will be able to see which tools ran during a turn in both CLI modes, while the underlying agent loop remains unchanged.

# AxerCode Iterative Agent Loop Design

## Goal

Upgrade the current single-round `ToolCallingAgent` into an iterative agent loop that can handle multiple rounds of `tool_calls`, stop safely, and preserve tool failure observations in conversation history.

## Scope

This step covers:

- iterative provider/tool/provider loops inside `axercode-agent`
- explicit maximum tool-round stopping conditions
- tool failure observations staying in-session instead of aborting the turn
- lightweight CLI bean wiring updates if needed

This step does **not** cover:

- provider-side tool schema advertisement to Ollama
- reflection or self-critique
- advanced planning commands
- Web or desktop integration

## Approaches Considered

### 1. Keep a recursive tool loop

Simple to write, but harder to reason about and test. Stack depth and stop conditions are less obvious.

### 2. Use an explicit iterative loop with max rounds

Makes stopping conditions explicit and keeps the turn state easy to inspect.

### 3. Jump directly to a full ReAct engine

Too broad for this step and mixes multiple future concerns into one change.

## Recommended Design

Use approach 2.

`ToolCallingAgent` should become an explicit loop:

1. append the user message once
2. call the provider
3. if the provider completes, finish the turn
4. if the provider returns `tool_calls`, execute them all
5. append all tool observations as `TOOL` messages
6. repeat until:
   - the provider completes, or
   - a configured maximum number of tool rounds is reached

If the maximum round count is reached while the provider still wants tools, return a clear assistant message describing that the loop stopped intentionally.

## Error Handling

Tool failures should remain first-class observations.

That means:

- unknown tool failures
- invalid arguments
- shell timeouts

should all still be appended as `TOOL` messages and then handed back to the provider on the next round.

This is better than aborting the whole turn because later model behavior often depends on seeing what went wrong.

## Configuration

Keep `maxToolRounds` inside the agent constructor for now.

The CLI Spring configuration can instantiate the agent with a stable default such as `3`.

That keeps the loop bounded without adding new user-facing configuration yet.

## Testing Strategy

Add tests for:

- plain completion without tools
- one tool round
- multiple tool rounds
- tool failure remaining in the conversation
- max-round stop behavior

## Outcome

After this step, AxerCode will have a bounded iterative agent loop that is materially closer to a real coding agent, while still stopping short of full reflection or retry logic.

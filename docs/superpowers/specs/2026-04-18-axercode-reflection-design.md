# AxerCode Reflection Design

## Goal

Add a first-pass reflection mechanism so the agent responds more intelligently after tool failures.

## Scope

This step adds:

- bounded reflection prompts after failed tool calls
- configurable reflection-round limits
- agent behavior that nudges the model to correct or abandon bad tool usage

This step does not add fully autonomous retries, summary compaction, or plan mode.

## Recommended Approach

Use temporary reflection guidance on the provider request immediately following a failed tool round.

Behavior:

- keep the existing `TOOL ... [FAILURE]` observation in the session
- before the next provider call, prepend a temporary `SYSTEM` message that explains a tool failed
- tell the model to either correct the tool call or answer directly without repeating the same mistake
- cap the number of reflection-guided rounds with a small configurable limit

Why this approach:

- it reuses the existing session and provider flow
- it does not mutate persisted history with internal agent reasoning
- it is easy to test through provider-request inspection

## Rejected Alternatives

### 1. No explicit reflection

This leaves the model with only a raw failure observation. Some models recover, but the agent is not actively guiding correction.

### 2. Automatic tool retries

This is too aggressive for the first iteration. The agent might repeat unsafe or invalid actions without sufficient model reasoning.

### 3. Persist reflection messages in conversation history

This would pollute `/history`, checkpoints, and restored sessions with internal orchestration instructions.

## Design

### 1. Reflection message

When at least one tool in the previous round failed, the next provider request may get a temporary `SYSTEM` message such as:

`A previous tool call failed. Read the TOOL failure output carefully. Either issue a corrected tool call or answer directly without repeating the same failed action.`

This message is not stored in the session. It exists only in the request window sent to the provider.

### 2. Reflection budget

The agent will track how many reflection-guided rounds have been used in the current turn.

If the configured reflection budget is exhausted:

- the agent keeps running
- the raw failure observation still remains in the session
- no extra reflection system message is injected

The existing `maxToolRounds` remains the hard stop for the whole tool loop.

### 3. Configuration

Expose `maxReflectionRounds` from CLI Spring config next to the existing agent limits.

## Files Expected To Change

- `D:\AeroCode1\axercode-agent\src\main\java\com\axercode\agent\ToolCallingAgent.java`
- `D:\AeroCode1\axercode-agent\src\test\java\com\axercode\agent\ToolCallingAgentTest.java`
- `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\config\AxerCodeAgentProperties.java`
- `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\config\ToolConfiguration.java`
- `D:\AeroCode1\axercode-cli\src\main\resources\application.yml`

## Verification Strategy

- agent tests for reflection-message injection after failure
- agent tests for respecting the reflection-round limit
- CLI/module wiring verification for the new config property
- full Maven test and package verification on JDK 21

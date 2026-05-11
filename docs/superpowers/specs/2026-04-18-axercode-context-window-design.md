# AxerCode Context Window Design

## Goal

Add a role-aware sliding window so long-running AxerCode sessions stop growing provider requests without losing the full persisted conversation history.

## Scope

This step trims only the message list that gets sent to the provider. It does not summarize older content, mutate SQLite history, or change checkpoint persistence.

## Recommended Approach

Use a role-aware sliding window:

- Keep all `SYSTEM` messages in the request window.
- Keep only the most recent configurable number of non-system messages.
- Apply the window immediately before each provider request.
- Preserve the full `SessionContext` in memory and storage.

Why this approach:

- It protects temporary focus messages and later plan-mode system instructions.
- It prevents unlimited prompt growth.
- It is simple enough to verify cleanly before moving on to compaction or summarization.

## Rejected Alternatives

### 1. Flat "last N messages"

This is too blunt. It can drop critical `SYSTEM` guidance and make behavior unstable as the conversation grows.

### 2. Summary-based compaction now

This would introduce prompt design, persistence strategy, and replay ambiguity in the same step. That is too much scope for Step 19.

## Design

### 1. Core trimming utility

Add a small core utility that accepts a `SessionContext` and returns a trimmed provider-facing snapshot.

Behavior:

- If the non-system message count is already within the limit, return the original context.
- Otherwise, keep:
  - every `SYSTEM` message in original order
  - the last `maxRecentMessages` non-system messages in original order
- Return a new `SessionContext` with the same `SessionId`

This utility stays separate from `SessionContext` itself so the session model remains a simple immutable snapshot object.

### 2. Agent integration

`ToolCallingAgent` should use the trimming utility right before creating each `ProviderRequest`.

This means:

- the live session and persisted session still keep the full message history
- each provider round gets a bounded request window
- tool-loop requests also benefit from the same trimming logic

### 3. Configuration

Expose the limits through CLI-side Spring configuration:

- `maxToolRounds`
- `maxRecentMessages`

The CLI currently owns the local runtime wiring, so this is the right place to configure the agent bean.

## Files Expected To Change

- `D:\AeroCode1\axercode-core\src\main\java\com\axercode\core\session\SessionContextWindow.java`
- `D:\AeroCode1\axercode-core\src\test\java\com\axercode\core\session\SessionContextWindowTest.java`
- `D:\AeroCode1\axercode-agent\src\main\java\com\axercode\agent\ToolCallingAgent.java`
- `D:\AeroCode1\axercode-agent\src\test\java\com\axercode\agent\ToolCallingAgentTest.java`
- `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\config\AxerCodeAgentProperties.java`
- `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\config\ToolConfiguration.java`
- `D:\AeroCode1\axercode-cli\src\main\resources\application.yml`

## Verification Strategy

- Core tests for no-op behavior, system-message preservation, and recent-message trimming
- Agent tests proving provider requests are trimmed while final session history remains complete
- Spring CLI configuration test if needed for bean wiring
- Full Maven test and package verification on JDK 21

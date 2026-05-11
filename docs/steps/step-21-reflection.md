# Step 21 - Reflection

## This Step Added

Step 21 adds the first bounded reflection/self-correction behavior to AxerCode.

The main result is:

- failed tool observations still stay in the session
- the next provider request can receive a temporary reflection system message
- the reflection hint is bounded by a configurable per-turn limit

## Why This Design

The key design decision in this step was to avoid automatic tool retries.

Instead of letting the agent silently mutate or retry tool calls on its own, the agent now does something much safer:

- preserve the actual failure observation
- tell the model that a tool failed
- ask it to either correct the tool call or answer directly

That makes the system more self-correcting without hiding behavior from the user or polluting saved history.

## How It Works

### Reflection budget

`ToolCallingAgent` now has a `maxReflectionRounds` limit alongside:

- `maxToolRounds`
- `SessionContextWindow`

This reflection budget applies only within a single conversation turn.

### Reflection trigger

When a tool round produces at least one `FAILURE` result:

- the failure observation is appended as a `TOOL` message, just like before
- the next provider request may receive a temporary `SYSTEM` reflection message

The reflection message says:

`A previous tool call failed. Read the TOOL failure output carefully. Either issue a corrected tool call or answer directly without repeating the same failed action.`

### Temporary request-only guidance

Just like focus-aware and checkpoint-aware prompting, the reflection message is request-only context.

It is not written into:

- the stored `SessionContext`
- `/history`
- SQLite checkpoints

That keeps the transcript honest and leaves internal agent guidance out of persisted user-visible history.

### Runtime config

The CLI runtime now exposes:

- `axercode.agent.max-reflection-rounds`

The default is `1`, which is enough for a first correction nudge without overcomplicating the loop.

## Verification Run

I verified this step with:

- `scripts\mvn-jdk21.cmd -q -pl axercode-agent -am test "-Dtest=ToolCallingAgentTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test`
- `scripts\mvn-jdk21.cmd -q -pl axercode-agent -am test`
- `scripts\mvn-jdk21.cmd -q test`
- `scripts\mvn-jdk21.cmd -q package`
- `C:\Program Files\Java\jdk-21\bin\java.exe -jar D:\AeroCode1\axercode-cli\target\axercode-cli-0.1.0-SNAPSHOT.jar --prompt "Reply with exactly REFLECTION_OK and nothing else."`

The packaged CLI returned:

- `REFLECTION_OK`

## What The Tests Proved

The new agent tests verify two specific reflection behaviors:

1. after a failed tool round, the next provider request contains the reflection system guidance
2. once the configured reflection budget is exhausted, later requests no longer receive that extra guidance

That means reflection is now explicit, bounded, and observable in tests.

## What This Unlocks Next

With Step 21 in place, AxerCode now has the first layer of self-correction on top of tool use:

- tool failure observations
- bounded reflection guidance
- preserved transcript integrity

This gives the later plan-mode and higher-level orchestration steps a much better base to build on.

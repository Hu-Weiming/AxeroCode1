# Step 12 - Iterative Agent Loop

## Step Goal

Upgrade the current tool-calling agent from a single extra tool round into a bounded iterative loop that can handle multiple sequential `tool_calls`.

## What Was Done

1. Extended `ToolCallingAgent` from a one-round tool loop to an explicit iterative loop.
2. Added a configurable `maxToolRounds` constructor parameter with a safe default.
3. Preserved tool failures as `TOOL` messages instead of aborting the turn.
4. Added tests for multiple tool rounds, failure observations, and max-round stopping.
5. Updated CLI Spring wiring to construct the agent with a stable default loop bound.

## Files Added or Changed

### Production Code

- `axercode-agent/src/main/java/com/axercode/agent/ToolCallingAgent.java`
- `axercode-cli/src/main/java/com/axercode/cli/config/ToolConfiguration.java`

### Tests

- `axercode-agent/src/test/java/com/axercode/agent/ToolCallingAgentTest.java`

### Documentation

- `docs/superpowers/specs/2026-04-18-axercode-iterative-agent-loop-design.md`
- `docs/superpowers/plans/2026-04-18-axercode-step-12-iterative-agent-loop.md`
- `docs/steps/step-12-iterative-agent-loop.md`

## How It Was Implemented

## 1. Explicit Iterative Loop

`ToolCallingAgent` now runs an explicit `while` loop instead of assuming there will be at most one tool-execution round.

Each turn now behaves like this:

1. append the user prompt once
2. call the provider with the current session history
3. if the provider completes, append the assistant reply and finish
4. if the provider returns `tool_calls`, execute them all
5. append every tool result as a `TOOL` message
6. repeat until completion or until the configured round limit is reached

This makes the stopping rule easy to test and easy to reason about.

## 2. Bounded Stop Condition

The agent now takes `maxToolRounds` in its constructor.

The default constructor still exists, but it now delegates to a default limit of `3`.

If the provider still returns `tool_calls` after the configured number of rounds, the agent stops intentionally and returns a clear assistant message:

`[AxerCode] Reached max tool rounds (N).`

That is more honest and safer than silently looping forever.

## 3. Tool Failure Observations Stay in the Session

This step intentionally keeps tool failures inside the conversation history.

That means failures such as:

- unknown tools
- bad arguments
- command execution failures

are all still converted into `TOOL` messages and sent back on the next provider round.

This is an important agent behavior because later model reasoning often depends on seeing what failed.

## 4. Stable Tool Observation Format

Tool results are still serialized into the same observation shape:

`TOOL <name> [<status>]`

followed by the tool output body.

Keeping that format stable matters because the provider and future prompt engineering work can rely on it.

## 5. CLI Wiring

The CLI Spring configuration now constructs `ToolCallingAgent` with a stable default limit of `3` rounds.

That keeps the bound centralized in the composition root without adding new user-facing flags yet.

## Why This Design

The key design decision in this step was to stop short of a full ReAct engine and instead strengthen the existing agent loop in one focused dimension: iteration.

That gives AxerCode a real capability improvement without mixing in too many future concerns:

- more than one tool round is now possible
- failure observations are preserved
- infinite loops are prevented
- the CLI still stays thin

## Important Limitation

Step 12 does **not** mean local Ollama is already fully choosing tools in real runtime.

The current provider layer still does not advertise full tool schema metadata to Ollama.

What this step guarantees is narrower and precise:

- if the provider returns `tool_calls`
- the agent can now execute multiple rounds
- preserve failures as observations
- and stop safely when the limit is reached

## TDD Evidence

This step followed red-green loops inside `ToolCallingAgentTest`:

1. tests for multiple tool rounds were added first
2. tests for failure observations were added next
3. a test for max-round stopping was added after that
4. the production loop was then expanded until all targeted tests passed

The implementation was only widened enough to satisfy the new behaviors.

## Verification Commands

The following commands were executed successfully in `D:\AeroCode1`:

```bat
scripts\mvn-jdk21.cmd -q -pl axercode-agent -am test
scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test
scripts\mvn-jdk21.cmd -q test
scripts\mvn-jdk21.cmd -q package
```

## Verification Note

During verification, running overlapping Maven commands in parallel produced a false Surefire discovery failure in `axercode-cli`.

Re-running the same commands sequentially resolved the issue and all verifications passed.

This step therefore also confirmed an execution rule for later work: do not run overlapping Maven test/package commands in parallel against the same workspace.

## Why This Step Matters

This is the first time AxerCode has a bounded iterative agent loop instead of a one-off provider-tool-provider shortcut.

It now supports:

- multiple sequential tool rounds
- bounded stopping behavior
- failure observations staying in context
- cleaner preparation for later reflection and retry strategies

## Next Step

Step 13 should expose more of the tool-aware agent behavior to the CLI interaction experience, likely by improving the shell feedback surface and preparing for plan-oriented or richer command flows.

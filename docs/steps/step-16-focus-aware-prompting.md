# Step 16 - Focus-Aware Prompting

## Step Goal

Make the persisted `/focus` path actually influence model turns by injecting it into the agent/provider request context, while keeping stored session history clean.

## What Was Done

1. Added a reusable `ShellContextAugmenter` in the CLI layer.
2. Updated `CliChatService` to read the current focus path from `ShellStateStore`.
3. Converted the focus path into a temporary `SYSTEM` message before the agent call.
4. Stripped the temporary focus message back out of the returned session before creating `CliChatTurn`.
5. Verified through tests and a packaged interactive run that focus affects the agent input but does not pollute `/history`.

## Files Added or Changed

### Production Code

- `axercode-cli/src/main/java/com/axercode/cli/service/ShellContextAugmenter.java`
- `axercode-cli/src/main/java/com/axercode/cli/service/CliChatService.java`

### Tests

- `axercode-cli/src/test/java/com/axercode/cli/service/CliChatServiceTest.java`
- `axercode-cli/src/test/java/com/axercode/cli/service/InteractiveShellServiceTest.java`

### Documentation

- `docs/superpowers/specs/2026-04-18-axercode-focus-aware-prompting-design.md`
- `docs/superpowers/plans/2026-04-18-axercode-step-16-focus-aware-prompting.md`
- `docs/steps/step-16-focus-aware-prompting.md`

## How It Was Implemented

## 1. Temporary Focus Context

The key implementation decision in this step was:

- let focus influence the model
- but do not persist focus as a permanent session message

`ShellContextAugmenter` now builds a temporary `SYSTEM` message when a focus path exists:

`Current focus path: <path>. Prefer this path when resolving relative project references for this turn.`

This message is concise and operational. It guides the model without pretending files were already read.

## 2. CLI-Layer Augmentation Instead of Agent Contract Expansion

The agent contract was intentionally left unchanged in Step 16.

Instead, `CliChatService` now:

1. reads temporary system messages from `ShellContextAugmenter`
2. prepends them to the `SessionContext` before calling the agent
3. receives the updated session back from the agent
4. strips the prepended temporary messages back out before returning `CliChatTurn`

That keeps the change small and avoids widening the agent API too early.

## 3. Clean Session History

This is the most important behavioral guarantee of Step 16.

The model sees the focus message.

But persisted history does not keep it.

That means:

- `/focus` can affect turns
- `/history` still shows only the real conversation
- SQLite session storage stays clean

This is a better shape for later long-context features than permanently writing shell metadata into the conversation log.

## Why This Design

The main design choice here was to treat focus as **ephemeral prompt context**, not as **conversation history**.

That creates a reusable pattern for future transient context features such as:

- plan mode state
- branch metadata
- temporary execution constraints

If those were all written into the persisted session, the conversation log would become noisy and harder to reason about.

## Important Limitation

Step 16 makes focus visible to the model request path, but it does **not** make focus magically deterministic in the model's output.

In other words:

- the system message is now present
- the provider receives it
- but the exact wording of the assistant's answer still depends on the model

So the strongest verification for this step is structural:

- tests confirm the focus message is injected
- tests confirm it is removed before persistence

Not every real model reply will visibly mention the focus path.

## TDD Evidence

This step followed red-green loops around three behaviors:

1. `CliChatService` prepending a focus system message before calling the agent
2. `CliChatService` removing the temporary focus message from the returned session
3. `InteractiveShellService` making `/focus` affect later prompt turns through the shared shell-state store

The first red run failed because `CliChatService` had no `ShellStateStore` integration and no context augmentation path.

Minimal production code was then added until the targeted tests passed.

## Verification Commands

The following commands were executed successfully in `D:\AeroCode1`:

```bat
scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test "-Dtest=CliChatServiceTest,InteractiveShellServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"
scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test
scripts\mvn-jdk21.cmd -q test
scripts\mvn-jdk21.cmd -q package
```

## Runtime Verification

The packaged interactive CLI was run successfully with a fresh SQLite database and shell history file:

```bat
@'
/focus D:\AeroCode1
hello
/history
/exit
'@ | "C:\Program Files\Java\jdk-21\bin\java.exe" "-Daxercode.storage.database-file=D:\AeroCode1\target\step16-shell-state.db" "-Daxercode.shell.history-file=D:\AeroCode1\target\step16-shell-history.txt" -jar D:\AeroCode1\axercode-cli\target\axercode-cli-0.1.0-SNAPSHOT.jar --interactive
```

Observed output confirmed:

- `/focus D:\AeroCode1` succeeded
- the prompt turn completed normally
- `/history` showed only:
  - `USER: hello`
  - `ASSISTANT: ...`

There was no persisted `SYSTEM:` focus message in history.

## Why This Step Matters

This is the first point where the advanced shell state starts influencing the actual model path instead of only being stored and displayed.

That makes `/focus` a real working feature rather than passive metadata.

## Next Step

Step 17 should build on this ephemeral-context pattern and start making tool-aware prompting stronger, likely by improving provider-side tool schema support or by introducing richer turn context for plan-oriented workflows.

# Step 13 - CLI Tool Feedback

## Step Goal

Expose executed tool results in the CLI so users can see what the agent actually did during a turn instead of only seeing the final assistant reply.

## What Was Done

1. Extended the CLI turn model so `toolResults` survive the agent-to-CLI boundary.
2. Added a dedicated CLI-side `ToolFeedbackFormatter`.
3. Printed tool feedback in the interactive shell before the assistant reply.
4. Printed tool feedback in one-shot `--prompt` mode before the assistant reply.
5. Verified that normal no-tool turns remain compact and still run correctly.

## Files Added or Changed

### Production Code

- `axercode-cli/src/main/java/com/axercode/cli/service/CliChatTurn.java`
- `axercode-cli/src/main/java/com/axercode/cli/service/CliChatService.java`
- `axercode-cli/src/main/java/com/axercode/cli/service/ToolFeedbackFormatter.java`
- `axercode-cli/src/main/java/com/axercode/cli/service/InteractiveShellService.java`
- `axercode-cli/src/main/java/com/axercode/cli/command/AxerCodeCliCommand.java`

### Tests

- `axercode-cli/src/test/java/com/axercode/cli/service/CliChatServiceTest.java`
- `axercode-cli/src/test/java/com/axercode/cli/service/InteractiveShellServiceTest.java`
- `axercode-cli/src/test/java/com/axercode/cli/command/AxerCodeCliCommandTest.java`

### Documentation

- `docs/superpowers/specs/2026-04-18-axercode-cli-tool-feedback-design.md`
- `docs/superpowers/plans/2026-04-18-axercode-step-13-cli-tool-feedback.md`
- `docs/steps/step-13-cli-tool-feedback.md`

## How It Was Implemented

## 1. Preserve Tool Results in `CliChatTurn`

Before this step, `CliChatService` threw away the agent's `toolResults`.

That meant the agent could execute tools successfully, but the CLI had no way to show them.

`CliChatTurn` now carries:

- updated `SessionContext`
- final assistant `reply`
- ordered `toolResults`

This keeps the CLI shell and one-shot command path aligned with the agent boundary.

## 2. Add a Dedicated Formatter

`ToolFeedbackFormatter` was added to keep display logic out of the shell control flow.

Each tool result is rendered as a compact block like:

`[tool] <name> <status>`

followed by the tool output body.

This keeps Step 13 focused on visibility while giving later steps a clean place to refine formatting.

## 3. Interactive Shell Feedback

`InteractiveShellService` now prints formatted tool feedback blocks before it prints the assistant reply for a prompt turn.

Slash commands such as `/help`, `/history`, `/new`, and `/exit` are unchanged.

If no tools ran in a turn, nothing extra is printed.

## 4. One-Shot Prompt Feedback

`AxerCodeCliCommand` now uses `CliChatService.askTurn(...)` instead of only asking for the final reply string.

That gives one-shot `--prompt` mode access to the same `toolResults` list, so it can print the same feedback blocks before the reply.

This keeps the display behavior consistent across both CLI entry points.

## Why This Design

The main design choice here was to expose existing information instead of inventing a new event system.

That keeps the change small and honest:

- the agent boundary stays stable
- the CLI gets much better observability
- later streaming work is not blocked

## Important Limitation

Step 13 improves CLI visibility, but it does **not** mean local Ollama is already guaranteed to choose tools in real runtime.

The current provider layer still needs later work for full tool schema advertisement.

So this step guarantees:

- if tool results exist
- the CLI will now show them clearly

It does **not** yet guarantee that a real local model turn will always produce tool calls.

## TDD Evidence

This step followed red-green loops around three user-visible behaviors:

1. `CliChatService` preserving tool results
2. interactive shell printing tool feedback
3. one-shot prompt mode printing tool feedback

The first red run failed because:

- `CliChatTurn` did not have `toolResults`
- the one-shot command path had no `askTurn(...)`
- tool feedback formatting did not exist yet

Minimal production code was then added until the targeted tests passed.

## Verification Commands

The following commands were executed successfully in `D:\AeroCode1`:

```bat
scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test "-Dtest=CliChatServiceTest,InteractiveShellServiceTest,AxerCodeCliCommandTest" "-Dsurefire.failIfNoSpecifiedTests=false"
scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test
scripts\mvn-jdk21.cmd -q test
scripts\mvn-jdk21.cmd -q package
```

## Runtime Verification

The packaged CLI was also run successfully with the pinned JDK 21:

```bat
"C:\Program Files\Java\jdk-21\bin\java.exe" -jar D:\AeroCode1\axercode-cli\target\axercode-cli-0.1.0-SNAPSHOT.jar --prompt hello
```

Observed output:

`Hello! How can I assist you today? Feel free to ask me any questions or let me know if you need help with anything specific.`

## Verification Note

Running the packaged jar with the system `java` command initially used Java `24.0.1` from PATH and showed a SQLite native-access warning.

Re-running with the pinned JDK 21 path removed that warning.

This confirms the project should continue to be run with the explicit JDK 21 path in this environment.

## Why This Step Matters

This is the first point where CLI users can see tool activity instead of only the final answer.

That makes AxerCode much easier to debug and trust, especially once later steps start exercising more tool-heavy flows.

## Next Step

Step 14 should move toward higher-level CLI workflows, likely by adding plan-oriented or advanced slash command behavior on top of the stronger shell and agent foundations.

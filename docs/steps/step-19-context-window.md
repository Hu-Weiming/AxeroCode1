# Step 19 - Context Window

## This Step Added

Step 19 adds the first bounded conversation window to AxerCode.

The main result is:

- provider requests no longer send unlimited history
- `SYSTEM` messages are preserved
- only the most recent configurable number of non-system messages are sent
- full session history is still preserved in memory and SQLite

## Why This Design

The important design choice in this step was to trim only the provider-facing request window, not the session itself.

That gives us two important properties:

1. Long conversations stop growing every prompt forever.
2. `/history`, checkpoints, and persistence still keep the full transcript.

This is the right intermediate step before doing true summary-based compaction.

## How It Works

### Core utility

`SessionContextWindow` is a small core helper that:

- keeps all `SYSTEM` messages
- counts non-system messages
- keeps only the last `maxRecentMessages` non-system items
- returns a trimmed `SessionContext` with the same `SessionId`

### Agent integration

`ToolCallingAgent` now trims the session immediately before creating each `ProviderRequest`.

That means:

- the first provider round is bounded
- later tool-loop rounds are also bounded
- recent tool observations remain eligible for the window

### CLI configuration

The CLI runtime now owns explicit agent settings through `AxerCodeAgentProperties`:

- `maxToolRounds`
- `maxRecentMessages`

This replaced one more hard-coded runtime choice in `ToolConfiguration`.

## Verification Run

I verified this step with:

- `scripts\mvn-jdk21.cmd -q -pl axercode-core -am test "-Dtest=SessionContextWindowTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `scripts\mvn-jdk21.cmd -q -pl axercode-agent -am test "-Dtest=ToolCallingAgentTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test`
- `scripts\mvn-jdk21.cmd -q test`
- `scripts\mvn-jdk21.cmd -q package`
- `C:\Program Files\Java\jdk-21\bin\java.exe -jar D:\AeroCode1\axercode-cli\target\axercode-cli-0.1.0-SNAPSHOT.jar --prompt "Reply with exactly WINDOW_OK and nothing else."`

The packaged CLI returned:

- `WINDOW_OK`

## Debugging Note

During verification, I hit a transient Maven/Surefire discovery failure while running full `test` and `package` in parallel. The dump showed `NoClassDefFoundError` during JUnit discovery, which is consistent with concurrent target-directory mutation. Re-running `test` sequentially passed cleanly.

That was an execution-artifact problem, not a code regression.

## What This Unlocks Next

With Step 19 in place, the next natural step is to move from bounded recency into smarter context management:

- richer prompt shaping around checkpoints and focus
- summary-based context compaction
- reflection and recovery on longer sessions

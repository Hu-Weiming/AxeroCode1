# AxerCode Session Diff Design

## Goal

Add a first-pass `/diff` workflow so the CLI can explain how the current session differs from a named checkpoint or branch.

## Scope

This step adds:

- a message-level session differ
- `/diff` slash commands
- diff rendering against checkpoints and branches
- support for using the active checkpoint or active branch as the default diff target

This step does not add Git diff or file-level code diffing.

## Recommended Approach

Treat diffing as a comparison between two `SessionContext` snapshots.

For the first pass, compare them by longest shared prefix of messages, using message `role` and `content` as identity. Ignore message ids because branching intentionally creates new ids.

The rendered diff should answer three questions:

- how much history is shared
- what messages only exist in the current session
- what messages only exist in the reference session

## Design

### 1. Core diff utility

Add a small core utility that compares two `SessionContext` values and returns:

- shared-prefix message count
- current-only tail messages
- reference-only tail messages

This keeps diffing reusable and independent from CLI formatting.

### 2. Slash command behavior

The shell should support:

- `/diff`
- `/diff checkpoint <name>`
- `/diff branch <name>`

Default resolution:

- if an active checkpoint exists, `/diff` uses it
- otherwise if an active branch exists, `/diff` uses it
- otherwise it returns a friendly guidance message

### 3. Rendering

Render a readable summary with:

- target label
- common-prefix count
- current-only count and lines
- reference-only count and lines

If both tails are empty, report that the two sessions are identical.

## Files Expected To Change

- `D:\AeroCode1\axercode-core\src\main\java\com\axercode\core\session\SessionContextDiffer.java`
- `D:\AeroCode1\axercode-core\src\main\java\com\axercode\core\session\SessionDiff.java`
- `D:\AeroCode1\axercode-core\src\test\java\com\axercode\core\session\SessionContextDifferTest.java`
- `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\shell\SlashCommandDispatcher.java`
- `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\InteractiveShellService.java`
- `D:\AeroCode1\axercode-cli\src\test\java\com\axercode\cli\shell\SlashCommandDispatcherTest.java`
- `D:\AeroCode1\axercode-cli\src\test\java\com\axercode\cli\service\InteractiveShellServiceTest.java`

## Verification Strategy

- core tests for identical sessions and diverging tails
- slash-command tests for default diff target, named checkpoint diff, named branch diff, and no-target guidance
- interactive shell test proving `/diff` is discoverable through help/output paths
- full Maven test and package verification on JDK 21
- packaged CLI runtime check using a persisted checkpoint

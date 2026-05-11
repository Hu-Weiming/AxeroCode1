# Step 8 - JLine Interactive Shell

## Step Goal

Upgrade the CLI interactive shell from a plain Reader loop to a `JLine`-backed terminal experience while preserving the Step 7 session flow, slash commands, and one-shot prompt mode.

## What Was Done

1. Added `JLine 3.30.6` to the shared build and CLI module.
2. Added `AxerCodeShellProperties` so the CLI can configure a shell history file path.
3. Extended `InteractiveShellService` with a JLine terminal path, slash-command completion, and persisted history.
4. Switched `AxerCodeCliCommand --interactive` to dispatch through the JLine entrypoint instead of the old Reader path.
5. Added regression coverage for terminal-backed shell behavior and Spring bean construction.
6. Verified the packaged CLI jar with a real interactive run and a persisted history file.

## Files Added or Changed

### Production Code

- `pom.xml`
- `axercode-cli/pom.xml`
- `axercode-cli/src/main/java/com/axercode/cli/bootstrap/AxerCodeCliApplication.java`
- `axercode-cli/src/main/java/com/axercode/cli/config/AxerCodeShellProperties.java`
- `axercode-cli/src/main/java/com/axercode/cli/command/AxerCodeCliCommand.java`
- `axercode-cli/src/main/java/com/axercode/cli/service/InteractiveShellService.java`
- `axercode-cli/src/main/resources/application.yml`

### Tests

- `axercode-cli/src/test/java/com/axercode/cli/service/InteractiveShellServiceTest.java`
- `axercode-cli/src/test/java/com/axercode/cli/command/AxerCodeCliCommandTest.java`

## How It Was Implemented

## 1. JLine Dependency and Shell Properties

The root build now exposes a shared `jline.version`, and the CLI module depends on `org.jline:jline`.

`AxerCodeShellProperties` was added so the history path is configurable from Spring Boot properties instead of being hard-coded in the shell service.

The default value is:

`%USERPROFILE%\.axercode\history\cli.history`

That keeps shell state local to the Windows workstation and avoids mixing it into the repo.

## 2. JLine Terminal Path in InteractiveShellService

`InteractiveShellService` now has two execution paths:

- a `runInteractive(...)` JLine path used by the real CLI
- a `run(Reader, PrintWriter, ...)` path kept for deterministic tests

The JLine path builds a `Terminal` and `LineReader`, then preserves the same shell semantics from Step 7:

- `/help`
- `/history`
- `/new`
- `/exit`
- normal text routed into `CliChatService`

This keeps the domain behavior stable while upgrading only the terminal input layer.

## 3. History and Completion

The JLine `LineReader` is configured with:

- `DefaultHistory`
- `LineReader.HISTORY_FILE`
- `StringsCompleter` for slash commands
- duplicate-history suppression
- disabled event expansion

That gives AxerCode a practical REPL baseline:

- arrow-key history
- editable input lines
- persisted command history
- basic slash-command completion

## 4. Command Dispatch and Spring Wiring

`AxerCodeCliCommand` now routes `--interactive` into `runInteractive(...)`.

During runtime verification, a Spring integration issue surfaced: `InteractiveShellService` had two constructors, and Spring could not select the dependency-injected one in the packaged application context.

That was fixed by marking the three-argument constructor as the injected path and adding a regression test that creates the bean through a Spring `AnnotationConfigApplicationContext`.

## Why This Design

The important design choice was to keep the shell behavior and session model separate from the terminal implementation.

That means:

- `CliChatService` still owns provider requests and reply normalization
- `InMemorySessionStore` still owns the current session snapshot
- `InteractiveShellService` only upgrades how the user types and navigates input

This separation keeps Step 8 narrow and prevents terminal-library concerns from leaking into session or provider code.

## TDD Evidence

This step followed red-green loops in two areas:

1. A command test first failed because `--interactive` still used the old Reader path.
2. A Spring wiring regression test first failed because `InteractiveShellService` could not be instantiated from the application context.
3. Minimal production changes were made until both targeted tests passed again.

## Verification Commands

The following commands were executed successfully in `D:\AeroCode1`:

```bat
scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test "-Dtest=AxerCodeCliCommandTest" "-Dsurefire.failIfNoSpecifiedTests=false"
scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test "-Dtest=InteractiveShellServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"
scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test
scripts\mvn-jdk21.cmd -q test
scripts\mvn-jdk21.cmd -q -pl axercode-cli -am package
```

Real runtime verification also passed:

```bat
@'
hello
/history
/new
/history
/exit
'@ | java "-Daxercode.shell.history-file=D:\AeroCode1\target\step8-history\cli.history" -jar D:\AeroCode1\axercode-cli\target\axercode-cli-0.1.0-SNAPSHOT.jar --interactive
```

Observed runtime behavior:

- the CLI started in interactive mode
- `hello` produced a real Ollama-backed reply
- `/history` printed the session transcript
- `/new` reset the in-memory session
- `/exit` exited cleanly
- the history file was written to `D:\AeroCode1\target\step8-history\cli.history`

## Why This Step Matters

This is the first point where AxerCode feels like a terminal tool rather than a plain command wrapper.

It now supports:

- a proper REPL editing experience
- persisted shell history
- slash-command completion
- runtime-safe Spring wiring for the interactive shell

## Next Step

Step 9 should add SQLite-backed conversation persistence so the CLI can restore and save session state beyond the lifetime of a single process.

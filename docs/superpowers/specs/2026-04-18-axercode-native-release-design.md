# AxerCode Native Release Design

## Goal

Finalize the AxerCode release pipeline by making GraalVM Native Image a first-class build track in the repository, while keeping the outcome honest on the current Windows machine.

## Current Machine Constraints

- JDK 21 is installed and working.
- `jpackage` is available and already produces a working desktop app-image.
- GraalVM is not installed.
- `native-image` is not installed.
- No visible MSVC / Visual Studio Build Tools are installed.

These constraints mean a real Windows native-image build cannot be claimed yet. The release pipeline can still be made real by:

1. wiring AOT/native build profiles into Maven
2. adding D-drive GraalVM install helpers
3. verifying Spring AOT on the JVM now
4. exposing friendly native preflight scripts for the blocked steps

## Scope

This step includes:

- Maven AOT/native build profiles for CLI and server
- GraalVM-on-D install helpers
- native build preflight scripts for CLI and server
- current-machine verification through AOT-on-JVM smoke runs
- release documentation

This step does not include:

- a successful Windows native-image artifact, unless all prerequisites are present
- installer polish beyond the current `jpackage` app-image
- replacement of the current desktop app-image path

## Architecture

### CLI Native Path

The CLI is the primary native target because it aligns most closely with the project goal of a single-file coding assistant executable.

Repository support should include:

- Spring Boot AOT generation for `axercode-cli`
- Graal native build plugin wiring
- an AOT-on-JVM verification script that runs `--help`
- a native preflight/build script that checks for `native-image` and the Windows linker toolchain

### Server Native Path

The server should also gain native readiness so the desktop/web runtime can later move off the fat jar.

Repository support should include:

- Spring Boot AOT generation for `axercode-server`
- Graal native build plugin wiring
- an AOT-on-JVM verification script that boots the local server and probes `/api/health`
- a native preflight/build script with the same toolchain checks

### GraalVM Installation Preference

To respect the machine preference, GraalVM install helpers should place the main runtime under `D:\GraalVM`, then set `GRAALVM_HOME` from there.

## Verification Strategy

### What Can Be Verified Now

- Maven still builds after native profile wiring
- AOT generation completes for CLI and server
- CLI runs under `-Dspring.aot.enabled=true`
- server runs under `-Dspring.aot.enabled=true`
- native preflight scripts fail clearly when GraalVM or MSVC are missing

### What Cannot Be Claimed Yet

Without GraalVM and Windows C++ prerequisites, the following must remain explicit blockers:

- `native-image` compilation
- a true Windows native `.exe` for CLI
- a true Windows native `.exe` for server

## Follow-On

After the machine has GraalVM and Windows build tools, the native build scripts should become executable without design changes. That makes this step the release-pipeline completion point, even if one environment-specific artifact remains blocked.

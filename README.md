# AxerCode

AxerCode is a Windows-first local AI coding assistant with CLI, web, and desktop entry points.

It is designed around a Claude Code / Codex style workflow: local conversation state, tool calling, SQLite persistence, and a simple path from terminal usage to a browser-based desktop experience.

## What Is In This Repository

- A Maven multi-module Java codebase for the core agent, providers, tools, storage, CLI, and server.
- A static web frontend served by the local Spring Boot server.
- Four ready-to-run Windows deliverables tracked under `AAAstart/`.
- Build scripts for JVM, native-image, `jpackage`, and Pake packaging flows.

## Current Capabilities

- Local Ollama chat flow with streaming responses.
- SQLite-backed session persistence.
- CLI one-shot and interactive modes.
- Web UI with multi-session sidebar and streaming chat.
- Desktop preview and `jpackage` app-image packaging.
- Anthropic provider integration.
- OpenAI-compatible provider slot reserved in the architecture, but the provider implementation is still a placeholder.

## Repository Layout

```text
axercode-core/               Shared message, tool, and provider domain models
axercode-provider-api/       Provider contracts and routing abstractions
axercode-provider-ollama/    Ollama integration
axercode-provider-anthropic/ Anthropic integration
axercode-provider-openai/    OpenAI-compatible provider placeholder
axercode-tools/              Built-in local tool implementations
axercode-storage-sqlite/     SQLite persistence
axercode-agent/              Tool-calling conversation orchestration
axercode-cli/                Terminal-first application
axercode-server/             Spring Boot server, APIs, and frontend
desktop/pake-shell/          Pake desktop shell workspace
scripts/                     Windows-focused build and launch scripts
AAAstart/                    Tracked final Windows artifacts
```

## Quick Start

### 1. Prerequisites

For source builds and local development:

- Windows 10 or Windows 11
- JDK 21
- Maven 3.9+
- Ollama running at `http://127.0.0.1:11434`
- The `qwen2.5:7b` model, or your own model name configured in the YAML files

Optional provider prerequisites:

- `ANTHROPIC_API_KEY` for the server-side Anthropic flow
- `OPENAI_API_KEY` for the server-side OpenAI-compatible route when that provider is fully implemented
- `AXERCODE_PROVIDER_ANTHROPIC_API_KEY` for CLI-side Anthropic usage
- `AXERCODE_PROVIDER_OPENAI_API_KEY` for CLI-side OpenAI-compatible usage

For rebuilding the packaged Windows artifacts:

- GraalVM 21 with `native-image`
- Visual Studio Build Tools with the Windows C/C++ toolchain
- Node.js
- Rust/Cargo

If you want the tracked desktop app-image under `AAAstart/` to download correctly from Git, install Git LFS before cloning.

### 2. Build the Project

```powershell
.\scripts\mvn-jdk21.cmd -q test
```

### 3. Run the Web/Desktop Preview

```powershell
.\scripts\launch-desktop-preview.ps1
```

That starts the local server with the `desktop` profile and opens `http://127.0.0.1:19090/`.

### 4. Run the CLI

One-shot prompt:

```powershell
.\scripts\mvn-jdk21.cmd -q -pl axercode-cli -am package
& 'C:\Program Files\Java\jdk-21\bin\java.exe' -jar .\axercode-cli\target\axercode-cli-0.1.0-SNAPSHOT.jar --prompt "Summarize this repository."
```

Interactive shell:

```powershell
& 'C:\Program Files\Java\jdk-21\bin\java.exe' -jar .\axercode-cli\target\axercode-cli-0.1.0-SNAPSHOT.jar --interactive
```

## Ready-To-Run Windows Artifacts

The repository also tracks four Windows deliverables under `AAAstart/`:

- `AAAstart/axercode-cli.exe/axercode-cli.exe`
- `AAAstart/axercode-server.exe/axercode-server.exe`
- `AAAstart/AxerCode.exe/AxerCode.exe`
- `AAAstart/AxerCode-Pake.exe/AxerCode-Pake.exe`

The desktop app-image contains one large runtime file tracked through Git LFS, so install Git LFS if you want the packaged desktop runtime to clone cleanly.

These are convenience artifacts for direct local use. If you want to rebuild them yourself, see [docs/getting-started.md](docs/getting-started.md).

## Configuration You May Need To Change

If you clone the repository on a different machine, the most common changes are:

- Change the default model name in `axercode-cli/src/main/resources/application.yml`.
- Change the default model or provider settings in `axercode-server/src/main/resources/application.yml`.
- Change the Ollama base URL if your local Ollama endpoint is not `http://127.0.0.1:11434`.
- Change the desktop preview port in `axercode-server/src/main/resources/application-desktop.yml` if `19090` is unavailable.
- Change the storage path if you do not want SQLite files written under `%USERPROFILE%\.axercode`.

## Documentation

- Setup, configuration, and rebuild guide: [docs/getting-started.md](docs/getting-started.md)
- Historical implementation notes: `docs/steps/`
- Internal implementation plans and specs: `docs/superpowers/`

## Security Notes

- Do not commit real provider keys into the YAML files.
- The server already expects `ANTHROPIC_API_KEY` and `OPENAI_API_KEY` as environment variables.
- The CLI can also receive provider settings through Spring Boot environment properties.

## License

No open-source license file has been added yet.

If you want outside users to legally use, modify, and redistribute AxerCode, add a license such as MIT or Apache-2.0 before treating this repository as a standard open-source project.

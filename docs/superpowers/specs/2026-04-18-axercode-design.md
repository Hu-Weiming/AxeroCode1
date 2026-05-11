# AxerCode Design

## What This Project Is

AxerCode is a local-first AI coding assistant that targets a Claude Code / Codex style workflow on Windows before expanding into cross-platform packaging.

The system will eventually support:

- a terminal-first CLI experience
- a Web UI backed by local streaming APIs
- a desktop shell built from the Web client
- local Ollama-hosted models plus OpenAI-compatible and Anthropic-style providers

## Step 1 Outcome

Step 1 does not create Java modules yet. Its purpose is to freeze the execution baseline so later implementation work is deterministic.

The baseline decisions are:

- the new project root is `D:\AeroCode1`
- `D:\AXERZONECODE` is ignored as a legacy half-finished workspace
- Java development must target JDK 21, not the machine-default Java 8 or Java 24 entries on PATH
- Maven execution must be wrapped so builds always use JDK 21
- the first available local model is `qwen2.5:7b` through Ollama
- Windows-first local execution is the primary goal for the first implementation window

## Recommended Architecture

### Core Direction

Use a unified domain core with thin delivery shells.

- CLI directly embeds the core modules for the fastest local feedback loop
- Web UI and desktop clients will later call a local Spring Boot service
- provider logic, storage, tools, and agent orchestration stay reusable

### Planned Module Boundaries

- `axercode-core`: shared domain model, prompts, context objects, configuration contracts
- `axercode-provider-ollama`: Ollama integration and streaming support
- `axercode-provider-api`: provider abstractions for OpenAI-compatible and Anthropic-style adapters
- `axercode-agent`: ReAct loop, structured output parsing, reflection, orchestration
- `axercode-tools`: file, directory, shell, diff, checkpoint tools
- `axercode-storage-sqlite`: local persistence for sessions, messages, and checkpoints
- `axercode-cli`: Picocli and JLine based REPL shell
- `axercode-server`: Spring Boot APIs, SSE endpoints, static asset delivery

## Data Flow

### CLI Flow

1. User enters input in the REPL.
2. Input router decides whether it is normal chat, slash command, or a future mode directive.
3. The agent assembles prompt context and tool availability.
4. The provider streams model output or requests tool execution.
5. Tool observations are fed back into the agent loop.
6. Final output is printed and persisted.

### Web Flow

1. Browser sends a conversation request to the local server.
2. Server delegates to the same core agent pipeline.
3. Incremental output is forwarded through SSE.
4. Session data is persisted in SQLite.

## Error Handling Strategy

- Treat provider timeouts, tool failures, and invalid structured output as recoverable states.
- Keep the first implementation simple, but always leave a place for retries and reflection.
- Never let shell commands or file tools execute without explicit validation boundaries.

## Testing Strategy

- Use TDD for production Java code once module implementation starts.
- Verify each new provider, tool, and agent behavior with focused automated tests.
- Run explicit baseline verification before beginning code generation tasks.

## Scope Control

The initial delivery window will not include:

- desktop packaging
- native image support
- advanced tool use breadth
- production-grade long-context compression

Those are planned later so the local Windows-first core can stabilize first.

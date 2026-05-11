# Step 2 - Git Initialization and Maven Multi-Module Skeleton

## Step Goal

Turn the empty AxerCode workspace into a real repository with a deterministic Maven multi-module skeleton that always validates under JDK 21.

## What Was Done

1. Initialized a new Git repository in `D:\AeroCode1`.
2. Created a dedicated working branch named `codex/step-2-skeleton` so implementation does not start on `main`.
3. Added the root Maven aggregator `pom.xml`.
4. Created the initial module skeleton for AxerCode's core, provider, tools, storage, agent, CLI, and server layers.
5. Updated the repository documentation so the module boundaries are visible from the top-level README.

## Why This Design

This structure is meant to keep the eventual system modular from the beginning:

- `axercode-core` stays free of infrastructure details
- provider abstractions are separated from provider implementations
- the agent layer can evolve independently from CLI and server delivery shells
- SQLite persistence remains replaceable
- Web and desktop support can reuse the `axercode-server` layer later

## Module Responsibilities

### `axercode-core`

Shared domain objects, context contracts, and common configuration primitives.

### `axercode-provider-api`

The neutral provider interface layer that the agent will depend on.

### `axercode-provider-ollama`

The concrete local provider implementation that will talk to `qwen2.5:7b` through Ollama.

### `axercode-tools`

The tool execution contracts and built-in tool implementations.

### `axercode-storage-sqlite`

The local persistence module for sessions, message history, checkpoints, and summaries.

### `axercode-agent`

The ReAct loop, structured output parsing, tool routing, and reflection behavior.

### `axercode-cli`

The terminal-first shell that will embed the core stack directly for fast local interaction.

### `axercode-server`

The Spring Boot service boundary that will later back the Web UI and desktop shell.

## Important Build Decisions

### JDK 21 Enforcement

The parent POM includes a Maven Enforcer rule that requires Java 21 and Maven 3.9+ during `validate`.

### Spring Boot Alignment

The parent POM imports the Spring Boot BOM at the parent level so later modules can adopt Spring-managed dependencies without repeating versions.

### Minimal Step Scope

This step intentionally avoids creating Java source files. The goal is to prove the build structure and dependency graph before implementation starts.

## Verification Plan

The correct verification for this step is not a unit test. It is a real Maven model validation under the JDK 21 wrapper:

- `scripts\mvn-jdk21.cmd -q validate`

If that command passes, the scaffold is structurally sound enough for the next step.

## Verification Result

The validation command was executed in `D:\AeroCode1` and completed successfully with exit code `0`. That confirms:

- the parent POM is syntactically valid
- every module is discoverable by the Maven reactor
- the runtime baseline is compatible with JDK 21
- the Maven enforcer rules do not reject the current toolchain

## Next Step

Step 3 will start turning this skeleton into an executable development base:

- add the first Java packages
- introduce the core domain model
- prepare the first compileable module code

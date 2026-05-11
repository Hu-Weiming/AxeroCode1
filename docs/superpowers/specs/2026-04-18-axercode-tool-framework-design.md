# AxerCode Tool Framework Design

## Goal

Build the first reusable tool framework for AxerCode so later Agent / ReAct steps can execute local actions through a stable interface instead of hard-coded branching.

## Scope

This step only covers the tool layer itself:

- tool definition metadata
- tool registration and lookup
- tool execution dispatch
- argument parsing and validation
- three built-in local tools

This step does **not** include:

- automatic model-driven tool invocation
- ReAct loops
- reflection or retry logic
- Web or desktop integration

## Approaches Considered

### 1. Dedicated `axercode-tools` framework with built-ins

Keep the framework isolated in `axercode-tools`, expose plain Java interfaces, and let later agent steps call it.

**Pros**

- clean module boundary
- easy to test without provider or CLI runtime
- does not force early decisions about Agent orchestration

**Cons**

- CLI will not yet auto-run tools

### 2. Integrate tools directly into the CLI shell now

Put tool dispatch into the CLI module and let the shell trigger local actions immediately.

**Pros**

- fast user-visible results

**Cons**

- wrong long-term boundary
- couples tool execution to one client shell
- likely refactor cost when Agent logic arrives

### 3. Wait and implement tools only inside the Agent loop later

Skip the dedicated tool framework now and build tools only when ReAct arrives.

**Pros**

- fewer files today

**Cons**

- agent code becomes harder to isolate and test
- no reusable contract for local actions

## Recommended Design

Use approach 1.

Add a small framework in `axercode-tools`:

- `AxerTool` interface for one executable tool
- `ToolDefinition` metadata model
- `ToolRegistry` for registration and discovery
- `ToolExecutor` for lookup and dispatch
- `ToolArguments` helper for parsing JSON arguments safely

Add three built-in tools:

- `read_file`
- `list_directory`
- `run_shell`

The tools should return existing `ToolExecutionResult` values from `axercode-core`.

## Responsibilities

### `AxerTool`

Owns:

- tool metadata
- execution for one tool

### `ToolRegistry`

Owns:

- listing available tool names
- preventing duplicate registrations
- resolving a tool by name

### `ToolExecutor`

Owns:

- finding the named tool
- executing it
- returning friendly failure results for unknown tools or invalid arguments

### Built-in tools

Own:

- local filesystem or shell behavior
- validation for their own required arguments

## Validation Rules

- `read_file` requires a `path`
- `list_directory` requires a `path` and accepts optional `recursive`
- `run_shell` requires a `command` and accepts optional `timeoutSeconds`

Invalid arguments should not throw raw stack traces to callers. They should become readable failure outputs in `ToolExecutionResult`.

## Testing Strategy

- registry tests for registration and duplicate protection
- executor tests for dispatch and unknown-tool handling
- per-tool tests for success and invalid-argument behavior
- shell tool test against a simple PowerShell command on Windows

## Outcome

After this step, AxerCode should have a stable tool layer that later provider and agent steps can consume without redesigning local action execution.

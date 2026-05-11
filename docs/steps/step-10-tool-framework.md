# Step 10 - Tool Framework

## Step Goal

Build the first reusable tool framework for AxerCode so later Agent and ReAct steps can execute local actions through a stable interface.

## What Was Done

1. Added `jackson-databind` and JUnit support to `axercode-tools`.
2. Added the core tool framework types:
   - `AxerTool`
   - `ToolDefinition`
   - `ToolRegistry`
   - `ToolExecutor`
   - `ToolArguments`
3. Added three built-in local tools:
   - `read_file`
   - `list_directory`
   - `run_shell`
4. Added tests for framework behavior and each built-in tool.
5. Verified the whole repository still builds and packages cleanly under JDK 21.

## Files Added or Changed

### Production Code

- `axercode-tools/pom.xml`
- `axercode-tools/src/main/java/com/axercode/tools/AxerTool.java`
- `axercode-tools/src/main/java/com/axercode/tools/ToolDefinition.java`
- `axercode-tools/src/main/java/com/axercode/tools/registry/ToolRegistry.java`
- `axercode-tools/src/main/java/com/axercode/tools/execution/ToolExecutor.java`
- `axercode-tools/src/main/java/com/axercode/tools/execution/ToolArguments.java`
- `axercode-tools/src/main/java/com/axercode/tools/builtin/ReadFileTool.java`
- `axercode-tools/src/main/java/com/axercode/tools/builtin/ListDirectoryTool.java`
- `axercode-tools/src/main/java/com/axercode/tools/builtin/RunShellTool.java`

### Tests

- `axercode-tools/src/test/java/com/axercode/tools/registry/ToolRegistryTest.java`
- `axercode-tools/src/test/java/com/axercode/tools/execution/ToolExecutorTest.java`
- `axercode-tools/src/test/java/com/axercode/tools/builtin/ReadFileToolTest.java`
- `axercode-tools/src/test/java/com/axercode/tools/builtin/ListDirectoryToolTest.java`
- `axercode-tools/src/test/java/com/axercode/tools/builtin/RunShellToolTest.java`

## How It Was Implemented

## 1. Framework Boundary

The framework is isolated inside `axercode-tools`.

That keeps the layering clean:

- `axercode-core` still owns shared cross-module contracts like `ToolCall` and `ToolExecutionResult`
- `axercode-tools` now owns tool definitions, registration, argument parsing, and concrete tool implementations

This was a deliberate choice so the CLI and the future Agent loop can both reuse the same tool layer.

## 2. Tool Definition and Registration

`AxerTool` is the minimal execution contract:

- `definition()`
- `execute(ToolCall)`

`ToolDefinition` stores the tool name, description, and JSON schema string.

`ToolRegistry` prevents duplicate tool names and provides:

- lookup by tool name
- sorted available tool names

That gives the project its first stable “tool catalog” concept.

## 3. Tool Execution and Argument Parsing

`ToolExecutor` delegates a `ToolCall` to the matching registered tool and returns a friendly failure result for unknown tools.

`ToolArguments` parses JSON once and exposes typed accessors like:

- `requiredString(...)`
- `optionalBoolean(...)`
- `optionalInt(...)`

This keeps input validation consistent across built-in tools instead of each tool parsing JSON differently.

## 4. Built-in Tools

### `ReadFileTool`

Reads the full text content of a local file from a required `path` argument.

### `ListDirectoryTool`

Lists files and directories under a path, with optional recursive listing.

The output format is intentionally stable:

- `[FILE] relative/path`
- `[DIR] relative/path`

### `RunShellTool`

Runs a local PowerShell command with a bounded timeout and captures merged output.

It returns:

- success with command output
- failure on timeout
- failure on non-zero exit

## Why This Design

The main design choice was to stop before Agent orchestration.

That means Step 10 gives AxerCode a usable tool layer without prematurely coupling:

- provider parsing
- agent decision loops
- CLI behavior

This keeps Step 10 focused on one responsibility: local action execution contracts.

## TDD Evidence

This step followed red-green cycles in three waves:

1. Framework tests first failed because the registry and executor classes did not exist.
2. `ReadFileToolTest` failed first because the tool and argument helper did not exist.
3. `ListDirectoryToolTest` and `RunShellToolTest` failed first because those tools did not exist.

Minimal production code was added after each red phase until the targeted tests passed.

## Verification Commands

The following commands were executed successfully in `D:\AeroCode1`:

```bat
scripts\mvn-jdk21.cmd -q -pl axercode-tools -am test "-Dtest=ToolRegistryTest,ToolExecutorTest" "-Dsurefire.failIfNoSpecifiedTests=false"
scripts\mvn-jdk21.cmd -q -pl axercode-tools -am test "-Dtest=ReadFileToolTest" "-Dsurefire.failIfNoSpecifiedTests=false"
scripts\mvn-jdk21.cmd -q -pl axercode-tools -am test "-Dtest=ListDirectoryToolTest,RunShellToolTest" "-Dsurefire.failIfNoSpecifiedTests=false"
scripts\mvn-jdk21.cmd -q -pl axercode-tools -am test
scripts\mvn-jdk21.cmd -q test
scripts\mvn-jdk21.cmd -q package
```

## Why This Step Matters

This is the first point where AxerCode can represent local actions as reusable, testable tools instead of ad-hoc logic.

It now supports:

- named tool registration
- tool lookup and dispatch
- friendly unknown-tool failures
- reusable JSON argument parsing
- local file reading
- directory listing
- bounded shell execution

## Next Step

Step 11 should connect this tool layer into the first Agent-style execution path so provider `tool_calls` can become actual local actions instead of placeholder text.

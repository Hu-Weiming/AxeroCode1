# Step 17 - Provider Tool Schema

## Step Goal

Pass structured tool definitions from the local tool registry into provider requests and map them into Ollama `/api/chat` `tools`.

## What Was Done

1. Moved shared `ToolDefinition` metadata into `axercode-core`.
2. Updated `ProviderRequest` to carry structured tool definitions instead of only tool names.
3. Updated `ToolRegistry` to expose structured available tools.
4. Updated `ToolCallingAgent` to include available tool definitions in every provider round.
5. Updated `OllamaChatProvider` to serialize those tool definitions into the `/api/chat` `tools` array.
6. Verified that packaged CLI runtime still works with the new provider request shape.

## Files Added or Changed

### Production Code

- `axercode-core/src/main/java/com/axercode/core/tool/ToolDefinition.java`
- `axercode-core/src/main/java/com/axercode/core/provider/ProviderRequest.java`
- `axercode-tools/src/main/java/com/axercode/tools/AxerTool.java`
- `axercode-tools/src/main/java/com/axercode/tools/registry/ToolRegistry.java`
- `axercode-tools/src/main/java/com/axercode/tools/builtin/ReadFileTool.java`
- `axercode-tools/src/main/java/com/axercode/tools/builtin/ListDirectoryTool.java`
- `axercode-tools/src/main/java/com/axercode/tools/builtin/RunShellTool.java`
- `axercode-agent/src/main/java/com/axercode/agent/ToolCallingAgent.java`
- `axercode-cli/src/main/java/com/axercode/cli/config/ToolConfiguration.java`
- `axercode-provider-ollama/src/main/java/com/axercode/provider/ollama/OllamaChatProvider.java`
- `axercode-provider-ollama/src/main/java/com/axercode/provider/ollama/OllamaChatRequest.java`

### Removed

- `axercode-tools/src/main/java/com/axercode/tools/ToolDefinition.java`

### Tests

- `axercode-core/src/test/java/com/axercode/core/provider/ProviderContractsTest.java`
- `axercode-tools/src/test/java/com/axercode/tools/registry/ToolRegistryTest.java`
- `axercode-tools/src/test/java/com/axercode/tools/execution/ToolExecutorTest.java`
- `axercode-agent/src/test/java/com/axercode/agent/ToolCallingAgentTest.java`
- `axercode-provider-ollama/src/test/java/com/axercode/provider/ollama/OllamaChatProviderTest.java`

### Documentation

- `docs/superpowers/specs/2026-04-18-axercode-provider-tool-schema-design.md`
- `docs/superpowers/plans/2026-04-18-axercode-step-17-provider-tool-schema.md`
- `docs/steps/step-17-provider-tool-schema.md`

## How It Was Implemented

## 1. Shared Tool Metadata in `core`

Before this step, `ToolDefinition` lived in `axercode-tools`.

That was too low in the dependency graph for provider contracts, because providers should not depend on the tools module just to understand the shape of a tool declaration.

So the shared definition moved into `axercode-core`.

This lets all of these layers reference the same contract cleanly:

- tools
- agent
- providers

## 2. Structured `ProviderRequest.availableTools`

`ProviderRequest.availableTools` now carries `List<ToolDefinition>` instead of `List<String>`.

This matters because a real provider tool declaration needs:

- name
- description
- JSON parameter schema

Tool names alone were not enough to build a valid Ollama `tools` payload.

## 3. `ToolRegistry` Now Exposes Structured Tools

`ToolRegistry` still exposes `availableToolNames()` for convenience, but it now also exposes:

- `availableTools()`

That returns sorted structured tool definitions, making it the natural bridge between the local tool layer and the provider layer.

## 4. Agent Now Advertises Tools to Providers

`ToolCallingAgent` now takes a `ToolRegistry` in addition to `ToolExecutor`.

On every provider round, it includes:

- current conversation messages
- structured available tools

inside `ProviderRequest`.

This is the runtime link that was missing before.

The agent was already able to react to `tool_calls`, but now the provider request can truthfully advertise which tools exist.

## 5. Ollama `tools` Mapping

`OllamaChatRequest` now serializes shared tool definitions into Ollama's expected shape:

- `type: "function"`
- `function.name`
- `function.description`
- `function.parameters`

`parameters` is parsed from the stored JSON schema string using Jackson, so the request body now carries real structured schema data instead of placeholder names.

## Why This Design

The most important design choice in Step 17 was to treat tool metadata as a **shared contract** rather than a **provider-specific detail**.

That keeps the dependency direction clean and creates a strong foundation for later:

- OpenAI-compatible provider tool mapping
- Anthropic tool declarations
- richer tool descriptions and schema evolution

## Important Limitation

Step 17 closes the tool-schema gap in provider requests, but it does **not** guarantee that the local model will now always choose tools well.

What this step guarantees is narrower and more important:

- the provider request now includes real structured tool declarations
- the agent loop now advertises available tools truthfully
- Ollama receives a valid `tools` array

Model quality and tool-selection behavior are still partly dependent on the model itself and later prompt tuning.

## TDD Evidence

This step followed red-green loops across four layers:

1. `ProviderContractsTest` failed until `ProviderRequest` could carry structured tool definitions
2. `ToolRegistryTest` failed until the registry exposed `availableTools()`
3. `ToolCallingAgentTest` failed until the agent included tool definitions in provider requests
4. `OllamaChatProviderTest` failed until the provider serialized `tools` into `/api/chat`

This sequence kept the implementation grounded in the exact runtime behavior we wanted to unlock.

## Verification Commands

The following commands were executed successfully in `D:\AeroCode1`:

```bat
scripts\mvn-jdk21.cmd -q -pl axercode-core,axercode-tools,axercode-agent,axercode-provider-ollama -am test "-Dtest=ProviderContractsTest,ToolRegistryTest,ToolExecutorTest,ToolCallingAgentTest,OllamaChatProviderTest" "-Dsurefire.failIfNoSpecifiedTests=false"
scripts\mvn-jdk21.cmd -q -pl axercode-core -am test
scripts\mvn-jdk21.cmd -q -pl axercode-tools -am test
scripts\mvn-jdk21.cmd -q -pl axercode-agent -am test
scripts\mvn-jdk21.cmd -q -pl axercode-provider-ollama -am test
scripts\mvn-jdk21.cmd -q test
scripts\mvn-jdk21.cmd -q package
```

## Runtime Verification

The packaged CLI was also run successfully with the pinned JDK 21:

```bat
"C:\Program Files\Java\jdk-21\bin\java.exe" -jar D:\AeroCode1\axercode-cli\target\axercode-cli-0.1.0-SNAPSHOT.jar --prompt hello
```

Observed output:

`Hello! How can I assist you today?`

That confirms the end-to-end CLI path still works after changing the provider request shape to include structured tools.

## Why This Step Matters

This is the first point where AxerCode's local runtime can honestly tell the provider what tools are available.

That makes the tool-aware agent path much closer to a real local coding agent instead of a partly disconnected prototype.

## Next Step

Step 18 should build on this provider-side truthfulness and improve user-visible runtime behavior, most likely through streaming output or stronger real-time tool-aware interaction paths.

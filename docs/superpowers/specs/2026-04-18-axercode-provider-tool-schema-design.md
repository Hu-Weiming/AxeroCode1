# AxerCode Provider Tool Schema Design

## Goal

Expose structured tool metadata to providers so local model requests can advertise which tools are available instead of only relying on hand-crafted test responses.

## Scope

This step covers:

- introducing a provider-safe structured tool definition model in the shared core layer
- passing available tool definitions from the agent into `ProviderRequest`
- mapping structured tool metadata into Ollama `/api/chat` `tools`

This step does **not** cover:

- streaming tool calls
- OpenAI-compatible or Anthropic provider implementations
- plan mode
- reflection or retry logic

## Problem

Right now the agent can execute `tool_calls`, but the provider request path still does not send real tool schema metadata to Ollama.

That means:

- local runtime cannot truthfully advertise tool availability to the model
- the current agent loop is stronger in tests than it is in real local execution
- provider integration is blocked from becoming a real tool-aware runtime

## Approaches Considered

### 1. Keep provider requests on `List<String>` tool names

Fastest to change in one place, but loses descriptions and JSON parameter schema, which are exactly what provider tool declarations need.

### 2. Add a shared structured tool definition model in `core`

Lets tools, agent, and providers talk about the same tool metadata without creating duplicate types in each module.

### 3. Let each provider read directly from `ToolRegistry`

Would leak tool-module concerns into provider modules and weaken module boundaries.

## Recommended Design

Use approach 2.

Create a shared core model for tool metadata and route it like this:

1. built-in tools expose the shared tool definition
2. `ToolRegistry` returns structured available tools
3. `ToolCallingAgent` includes them in `ProviderRequest`
4. `OllamaChatProvider` maps them into the `tools` array for `/api/chat`

## Model Placement

The current `ToolDefinition` lives in `axercode-tools`, which is too low-level for provider contracts.

Move the shared definition into `axercode-core` so it can be referenced by:

- `axercode-tools`
- `axercode-agent`
- `axercode-provider-*`

That keeps the dependency direction clean.

## Ollama Mapping

For Ollama, each shared tool definition should map to:

- `type: "function"`
- `function.name`
- `function.description`
- `function.parameters`

Where `parameters` is parsed from the stored JSON schema string.

## Why This Design

The key design choice is making tool metadata a first-class shared contract instead of a provider-specific detail.

That gives AxerCode a reusable provider boundary for later:

- OpenAI-compatible `tools`
- Anthropic tool use
- richer tool descriptions and schemas

## Testing Strategy

Add tests for:

- `ProviderRequest` defensively copying structured tool definitions
- `ToolRegistry` returning structured available tools
- `ToolCallingAgent` including those tools in provider requests
- `OllamaChatProvider` serializing structured tools into `/api/chat` requests

## Outcome

After this step, AxerCode will move from “agent can process tool calls” to “provider requests can truthfully advertise the available tool set,” which is the missing runtime link for local tool-aware inference.

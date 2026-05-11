# AxerCode CLI Streaming Design

## Goal

Add token streaming to the local CLI experience without breaking the existing synchronous provider, agent, and shell paths.

## Scope

This step adds a new optional streaming path for provider calls and CLI rendering. It does not redesign the whole agent loop, SSE server, or Web UI. The non-streaming path remains the default fallback for providers that do not support streaming.

## Design Summary

### 1. Provider-level streaming contract

`LlmProvider` will gain a streaming entry point that accepts a `ProviderRequest` plus a callback for text deltas and returns the final `ProviderResponse` after the stream completes.

Why:

- The existing `generate(...)` path remains stable.
- Providers can choose whether they support streaming.
- The caller receives both live text deltas and the final structured response.

### 2. Ollama `/api/chat` NDJSON streaming adapter

`OllamaChatProvider` will implement the new streaming method by calling `/api/chat` with `stream=true`, then reading line-delimited JSON chunks from the response body.

Behavior:

- For each chunk with `message.content`, forward the delta to the callback.
- Keep reading until the final `done=true` chunk arrives.
- Map the final chunk to `ProviderResponse.complete(...)` or `ProviderResponse.toolCalls(...)`.
- Preserve the current `generate(...)` behavior for non-streaming requests.

### 3. Agent-level streaming hook

`ConversationAgent` will gain an optional streaming entry point. `ToolCallingAgent` will implement it by using provider streaming for each provider round and preserving the existing bounded tool loop.

Behavior:

- The user prompt is still appended to session history first.
- Each provider round can stream assistant text deltas.
- If a round finishes with `TOOL_CALLS`, the tool loop continues exactly as before.
- If a round finishes with `COMPLETE`, the final assistant reply is appended to the session and returned.

Limitation for this step:

- If a model emits visible text and then ends the same round with `tool_calls`, those deltas may already have been printed to the CLI even though they are not persisted as a final assistant message. This is acceptable for Step 18 and can be refined in a later step.

### 4. CLI progressive rendering

`CliChatService` will expose streaming turn methods. `AxerCodeCliCommand` and `InteractiveShellService` will pass a writer callback so the user sees output as tokens arrive instead of waiting for the full reply.

Behavior:

- Tool feedback blocks still print after tool execution finishes.
- The final reply line still ends cleanly with a newline.
- If a provider does not support streaming, the CLI falls back to the existing synchronous path.

## Files Expected To Change

- `D:\AeroCode1\axercode-provider-api\src\main\java\com\axercode\provider\api\LlmProvider.java`
- `D:\AeroCode1\axercode-provider-ollama\src\main\java\com\axercode\provider\ollama\OllamaChatProvider.java`
- `D:\AeroCode1\axercode-provider-ollama\src\main\java\com\axercode\provider\ollama\OllamaChatResponse.java`
- `D:\AeroCode1\axercode-provider-ollama\src\test\java\com\axercode\provider\ollama\OllamaChatProviderTest.java`
- `D:\AeroCode1\axercode-agent\src\main\java\com\axercode\agent\ConversationAgent.java`
- `D:\AeroCode1\axercode-agent\src\main\java\com\axercode\agent\ToolCallingAgent.java`
- `D:\AeroCode1\axercode-agent\src\test\java\com\axercode\agent\ToolCallingAgentTest.java`
- `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\CliChatService.java`
- `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\service\InteractiveShellService.java`
- `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\command\AxerCodeCliCommand.java`
- `D:\AeroCode1\axercode-cli\src\test\java\com\axercode\cli\service\InteractiveShellServiceTest.java`
- `D:\AeroCode1\axercode-cli\src\test\java\com\axercode\cli\command\AxerCodeCliCommandTest.java`

## Verification Strategy

- Provider contract tests for streaming support behavior.
- Ollama provider tests with a local HTTP server returning NDJSON chunks.
- Agent tests proving streamed text plus final persisted reply.
- CLI tests proving one-shot and interactive output render streamed deltas progressively.
- Full Maven test and package verification on JDK 21.

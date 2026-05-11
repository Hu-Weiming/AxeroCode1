# Step 18 - CLI Streaming

## This Step Added

Step 18 adds the first end-to-end token streaming path to AxerCode.

The main result is:

- `LlmProvider` now has an optional streaming contract.
- `OllamaChatProvider` now reads `/api/chat` NDJSON chunks and forwards text deltas.
- `ToolCallingAgent` now has a streaming entry point.
- `CliChatService`, the one-shot CLI command, and the interactive shell now render text progressively when the provider supports it.

## Why This Design

The key design choice in this step was to add streaming as a parallel path instead of rewriting the existing synchronous flow.

That gives us three benefits:

1. The old synchronous path stays intact as a fallback.
2. The streaming implementation stays small enough to test cleanly.
3. Later steps can extend streaming to richer event types without undoing this work.

## How It Works

### Provider API

`LlmProvider.streamGenerate(...)` accepts a `ProviderRequest` and a text-delta callback, then returns a final `ProviderResponse` when the stream ends.

That means callers get both:

- live token updates
- the same structured stop reason and tool-call information they already use in synchronous mode

### Ollama Provider

`OllamaChatProvider` now:

- sends `/api/chat` requests with `stream=true`
- reads line-delimited JSON chunks
- forwards each `message.content` delta to the callback
- accumulates the final assistant text
- still maps final `tool_calls` into `ProviderResponse.toolCalls(...)`

### Agent

`ToolCallingAgent` now exposes `continueConversationStreaming(...)`.

For this step, the agent streams the first provider round when streaming is available. If that round ends with plain assistant text, the CLI gets progressive output. If tools are requested, the agent keeps the existing bounded tool loop and finishes the later rounds synchronously.

This is an intentional scope boundary for Step 18.

### CLI

`AxerCodeCliCommand` and `InteractiveShellService` now pass writer callbacks into `CliChatService`.

The CLI prints text deltas immediately and avoids printing the final reply twice by comparing the streamed buffer with the final `CliChatTurn.reply()`.

## Important Limitation

Step 18 does not yet stream the whole multi-round tool workflow. The first provider round can stream, but turns that continue after tool execution still complete through the existing synchronous fallback path.

That keeps the current tool feedback ordering stable and avoids mixing a large protocol redesign into this step.

## Verification Run

I verified this step with:

- `scripts\mvn-jdk21.cmd -q -pl axercode-provider-api -am test`
- `scripts\mvn-jdk21.cmd -q -pl axercode-provider-ollama -am test`
- `scripts\mvn-jdk21.cmd -q -pl axercode-agent -am test`
- `scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test`
- `scripts\mvn-jdk21.cmd -q test`
- `scripts\mvn-jdk21.cmd -q package`
- `C:\Program Files\Java\jdk-21\bin\java.exe -jar D:\AeroCode1\axercode-cli\target\axercode-cli-0.1.0-SNAPSHOT.jar --prompt "Respond with exactly STREAM_OK and nothing else."`

The packaged CLI returned:

- `STREAM_OK`

## What This Unlocks Next

With Step 18 in place, the next natural step is to improve the intelligence around long conversations and richer streamed workflows:

- context window management
- context compaction
- more refined streaming across tool-driven turns
- later SSE reuse for server and Web UI

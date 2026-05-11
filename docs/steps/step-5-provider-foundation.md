# Step 5 - Provider Abstraction and Minimal Ollama Adapter

## Step Goal

Create the first real provider layer for AxerCode by defining a unified provider abstraction and implementing a minimal local Ollama `/api/chat` adapter.

## What Was Done

1. Added the shared `LlmProvider` abstraction to `axercode-provider-api`.
2. Added a provider-specific runtime exception type, `ProviderException`.
3. Added the first compileable `axercode-provider-ollama` implementation, `OllamaChatProvider`.
4. Added Ollama request/response DTOs for the `/api/chat` integration path.
5. Added tests for provider abstraction defaults, error metadata, plain text completion mapping, tool-call mapping, and HTTP error wrapping.

## Files Added or Changed

### Provider API

- `axercode-provider-api/pom.xml`
- `axercode-provider-api/src/main/java/com/axercode/provider/api/LlmProvider.java`
- `axercode-provider-api/src/main/java/com/axercode/provider/api/ProviderException.java`
- `axercode-provider-api/src/test/java/com/axercode/provider/api/LlmProviderContractTest.java`
- `axercode-provider-api/src/test/java/com/axercode/provider/api/ProviderExceptionTest.java`

### Ollama Adapter

- `axercode-provider-ollama/pom.xml`
- `axercode-provider-ollama/src/main/java/com/axercode/provider/ollama/OllamaChatProvider.java`
- `axercode-provider-ollama/src/main/java/com/axercode/provider/ollama/OllamaChatRequest.java`
- `axercode-provider-ollama/src/main/java/com/axercode/provider/ollama/OllamaChatResponse.java`
- `axercode-provider-ollama/src/test/java/com/axercode/provider/ollama/OllamaChatProviderTest.java`

## How It Was Implemented

## 1. Unified Provider Boundary

`LlmProvider` is now the shared contract every future provider adapter will implement. It exposes:

- `providerName()`
- `generate(ProviderRequest request)`
- `supportsStreaming()`, which currently defaults to `false`

This means later modules can depend on one stable abstraction instead of hard-coding Ollama details.

## 2. Friendly Provider Errors

`ProviderException` keeps both:

- provider name
- operation name

That gives the CLI and future Web layers a better way to present transport or parsing failures than generic runtime exceptions.

## 3. Minimal Ollama `/api/chat` Path

`OllamaChatProvider` currently supports a focused subset of the Ollama chat integration:

- send a normalized non-streaming chat request to `/api/chat`
- map a normal assistant text response into `ProviderResponse.complete(...)`
- map returned `tool_calls` into `ProviderResponse.toolCalls(...)`
- wrap transport and parsing failures in `ProviderException`

The implementation currently rejects `request.stream() == true` with a clear message because streaming is not implemented yet. That is an intentional scope boundary, not an accidental omission.

## 4. Spring Boot 3.x Compatibility

This step keeps the repository aligned with your `Spring Boot 3.x+` rule:

- the root BOM is still `Spring Boot 3.3.4`
- `axercode-provider-ollama` uses `spring-web` from the Spring 6 / Boot 3 line
- no dependency was added that would pull the project back to Spring 2.x

## TDD Evidence

This step again followed red-green cycles:

1. `provider-api` tests failed first because `LlmProvider` and `ProviderException` did not exist.
2. Minimal provider API code was added and the tests passed.
3. `OllamaChatProviderTest` failed first because `OllamaChatProvider` did not exist.
4. Minimal Ollama production code was added and the targeted tests passed.

## Verification Commands

The following commands were executed successfully in `D:\AeroCode1`:

```bat
scripts\mvn-jdk21.cmd -q -pl axercode-provider-api -am test
scripts\mvn-jdk21.cmd -q -pl axercode-provider-ollama -am test "-Dtest=OllamaChatProviderTest" "-Dsurefire.failIfNoSpecifiedTests=false"
scripts\mvn-jdk21.cmd -q -pl axercode-provider-ollama -am test
scripts\mvn-jdk21.cmd -q test
```

## Why This Step Matters

At this point AxerCode no longer only has domain contracts. It now has the first actual integration boundary and the first local model adapter.

That is the bridge between:

- internal session / tool / provider contracts
- real local model execution through Ollama

## Next Step

Step 6 should push toward the first executable application shell:

- define the CLI module dependencies with Spring Boot 3.x
- add the first Spring Boot console bootstrap
- start wiring `LlmProvider` and `OllamaChatProvider` into a minimal interactive path

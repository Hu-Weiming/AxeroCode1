# AxerCode Server + SSE Skeleton Design

## Goal

Turn `axercode-server` into a real Spring Boot 3.x local service that exposes the existing agent/provider/session capabilities over HTTP for future Web UI and desktop clients.

## Scope

This step adds:

- a bootable Spring Boot server module
- shared provider/agent/storage/tool bean wiring inside the server module
- a health endpoint
- a synchronous chat endpoint
- an SSE streaming chat endpoint
- session fetch by id for future Web UI hydration

This step does not add the browser UI itself.

## Recommended Approach

Keep the server thin and reuse the existing core stack:

- `ConversationAgent` remains the orchestration engine
- `SqliteSessionRepository` remains the session source of truth
- the server adds only transport-facing DTOs, controllers, and a small service

For streaming, use Spring MVC `SseEmitter` so the server can emit token deltas and a final completion event without introducing WebFlux yet.

## Design

### 1. Transport layer

Add a small REST controller with:

- `GET /api/health`
- `POST /api/chat`
- `POST /api/chat/stream`
- `GET /api/sessions/{sessionId}`

The synchronous endpoint returns one JSON response containing the updated session id, assistant reply, and summarized tool results.

The streaming endpoint emits:

- `token` events for incremental assistant text
- a final `complete` event containing the same final response shape as the sync endpoint

### 2. Server conversation service

Add a server-local service that:

- resolves the effective model
- loads an existing session when `sessionId` is provided
- creates a fresh session when it is omitted
- delegates to `ConversationAgent`
- persists the updated session through `SqliteSessionRepository`

This keeps controllers free of business logic and avoids coupling the server to the CLI shell abstractions.

### 3. Bean wiring

Because the current provider/agent/storage configuration lives under `axercode-cli`, the server module needs its own minimal Spring configuration classes for:

- provider properties and `LlmProvider`
- agent properties and `ConversationAgent`
- storage properties and `SqliteSessionRepository`
- tool beans, registry, and executor

This duplicates a small amount of configuration for now, but keeps the server module independent from CLI-only concepts such as shell state.

## Files Expected To Change

- `D:\AeroCode1\axercode-server\pom.xml`
- `D:\AeroCode1\axercode-server\src\main\java\com\axercode\server\bootstrap\AxerCodeServerApplication.java`
- `D:\AeroCode1\axercode-server\src\main\java\com\axercode\server\config\*.java`
- `D:\AeroCode1\axercode-server\src\main\java\com\axercode\server\api\*.java`
- `D:\AeroCode1\axercode-server\src\main\java\com\axercode\server\service\*.java`
- `D:\AeroCode1\axercode-server\src\main\resources\application.yml`
- `D:\AeroCode1\axercode-server\src\test\java\com\axercode\server\service\ServerConversationServiceTest.java`
- `D:\AeroCode1\axercode-server\src\test\java\com\axercode\server\api\AxerCodeChatControllerTest.java`

## Verification Strategy

- service tests for new-session load, existing-session load, sync persistence, and streaming persistence
- MVC tests for health, sync chat, stream chat, and session fetch
- full `axercode-server` module test and package verification on JDK 21
- packaged server boot verification and an HTTP health check against the running process

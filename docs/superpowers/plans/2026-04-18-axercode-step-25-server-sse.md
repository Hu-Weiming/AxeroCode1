# Step 25 Server + SSE Skeleton Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a bootable Spring Boot server module with sync chat, SSE chat, and session fetch endpoints for the future Web UI.

**Architecture:** Add server-local configuration for provider/agent/storage/tool wiring, a small conversation service for session orchestration and persistence, and REST/SSE controllers layered on top.

**Tech Stack:** Java 21, Spring Boot 3.3.4, Spring MVC, SSE (`SseEmitter`), SQLite JDBC, JUnit 5, MockMvc

---

### Task 1: Add server module dependencies and failing transport tests

**Files:**
- Modify: `D:\AeroCode1\axercode-server\pom.xml`
- Create: `D:\AeroCode1\axercode-server\src\test\java\com\axercode\server\api\AxerCodeChatControllerTest.java`

- [ ] Write failing MVC tests for health, sync chat, stream chat, and session fetch.
- [ ] Run `scripts\mvn-jdk21.cmd -q -pl axercode-server -am test "-Dtest=AxerCodeChatControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false"` and confirm red.
- [ ] Add the minimal Spring Boot server dependencies needed for those tests.
- [ ] Re-run the same command and confirm it still fails for missing production classes, not test setup.

### Task 2: Add server service and failing service tests

**Files:**
- Create: `D:\AeroCode1\axercode-server\src\main\java\com\axercode\server\service\ServerConversationService.java`
- Create: `D:\AeroCode1\axercode-server\src\main\java\com\axercode\server\service\ServerConversationTurn.java`
- Create: `D:\AeroCode1\axercode-server\src\test\java\com\axercode\server\service\ServerConversationServiceTest.java`

- [ ] Write failing tests for new sessions, existing sessions, sync persistence, and streaming persistence.
- [ ] Run `scripts\mvn-jdk21.cmd -q -pl axercode-server -am test "-Dtest=ServerConversationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"` and confirm red.
- [ ] Implement the minimal service.
- [ ] Re-run the same command and confirm green.

### Task 3: Add Spring Boot app, config, DTOs, and controllers

**Files:**
- Create: `D:\AeroCode1\axercode-server\src\main\java\com\axercode\server\bootstrap\AxerCodeServerApplication.java`
- Create: `D:\AeroCode1\axercode-server\src\main\java\com\axercode\server\config\*.java`
- Create: `D:\AeroCode1\axercode-server\src\main\java\com\axercode\server\api\*.java`
- Create: `D:\AeroCode1\axercode-server\src\main\resources\application.yml`

- [ ] Implement the minimal app bootstrap, properties, bean wiring, DTOs, and controller endpoints.
- [ ] Re-run the MVC test command and confirm green.

### Task 4: Verify and document Step 25

**Files:**
- Create: `D:\AeroCode1\docs\steps\step-25-server-sse.md`

- [ ] Run `scripts\mvn-jdk21.cmd -q -pl axercode-server -am test`.
- [ ] Run `scripts\mvn-jdk21.cmd -q test`.
- [ ] Run `scripts\mvn-jdk21.cmd -q package`.
- [ ] Start the packaged server with JDK 21 and confirm `/api/health` responds successfully.
- [ ] Write the Step 25 learning summary.

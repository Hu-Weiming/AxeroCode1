# Step 26 Minimal Web UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Serve a minimalist white browser chat UI from `axercode-server` and connect it to the existing streaming chat endpoint.

**Architecture:** Add a static HTML/CSS/JavaScript shell under Spring Boot static resources and use browser `fetch` streaming to consume the existing SSE-style response body from `/api/chat/stream`.

**Tech Stack:** Java 21, Spring Boot 3.3.4, static HTML/CSS/JavaScript, MockMvc, SSE over fetch streaming

---

### Task 1: Add failing web-shell tests

**Files:**
- Create: `D:\AeroCode1\axercode-server\src\test\java\com\axercode\server\api\AxerCodeWebUiTest.java`

- [ ] Write failing tests for `GET /`, `/assets/styles.css`, and `/assets/app.js`.
- [ ] Run `scripts\mvn-jdk21.cmd -q -pl axercode-server -am test "-Dtest=AxerCodeWebUiTest" "-Dsurefire.failIfNoSpecifiedTests=false"` and confirm red.

### Task 2: Add the minimal web shell

**Files:**
- Create: `D:\AeroCode1\axercode-server\src\main\resources\static\index.html`
- Create: `D:\AeroCode1\axercode-server\src\main\resources\static\assets\styles.css`
- Create: `D:\AeroCode1\axercode-server\src\main\resources\static\assets\app.js`

- [ ] Implement the smallest static page that satisfies the tests.
- [ ] Re-run the targeted web-shell test and confirm green.

### Task 3: Connect the page to server streaming

**Files:**
- Modify: `D:\AeroCode1\axercode-server\src\main\resources\static\assets\app.js`

- [ ] Add streaming POST behavior to `/api/chat/stream`.
- [ ] Render user messages, assistant streaming text, and final session tracking.
- [ ] Keep the page disabled while a request is in flight.

### Task 4: Verify and document Step 26

**Files:**
- Create: `D:\AeroCode1\docs\steps\step-26-web-ui.md`

- [ ] Run `scripts\mvn-jdk21.cmd -q -pl axercode-server -am test`.
- [ ] Run `scripts\mvn-jdk21.cmd -q test`.
- [ ] Run `scripts\mvn-jdk21.cmd -q package`.
- [ ] Start the packaged server and confirm `GET /` returns the web page.
- [ ] Write the Step 26 learning summary.

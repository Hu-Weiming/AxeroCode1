# Step 27 Web UI Productization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the minimalist Web UI feel more usable by adding session continuity, tool feedback, session status, and better error presentation.

**Architecture:** Keep the UI as static assets served by Spring Boot, persist only `sessionId` in browser storage, rehydrate with the existing session endpoint, and render tool feedback from the final streaming payload.

**Tech Stack:** Java 21, Spring Boot 3.3.4, static HTML/CSS/JavaScript, MockMvc, browser `localStorage`

---

### Task 1: Add failing web-shell tests for the richer UI markers

**Files:**
- Modify: `D:\AeroCode1\axercode-server\src\test\java\com\axercode\server\api\AxerCodeWebUiTest.java`

- [ ] Write failing tests for session-status markup, the reset action, and the richer asset markers.
- [ ] Run `scripts\mvn-jdk21.cmd -q -pl axercode-server -am test "-Dtest=AxerCodeWebUiTest" "-Dsurefire.failIfNoSpecifiedTests=false"` and confirm red.

### Task 2: Add session meta shell and styling

**Files:**
- Modify: `D:\AeroCode1\axercode-server\src\main\resources\static\index.html`
- Modify: `D:\AeroCode1\axercode-server\src\main\resources\static\assets\styles.css`

- [ ] Implement the new meta row and tool-note styles.
- [ ] Re-run the targeted web-shell test and confirm progress toward green.

### Task 3: Add session continuity, tool rendering, and error handling

**Files:**
- Modify: `D:\AeroCode1\axercode-server\src\main\resources\static\assets\app.js`

- [ ] Persist `sessionId` to `localStorage`.
- [ ] Rehydrate sessions through `GET /api/sessions/{sessionId}`.
- [ ] Render tool feedback from the `complete` payload.
- [ ] Add `New Session` reset behavior.
- [ ] Re-run the targeted web-shell test and confirm green.

### Task 4: Verify and document Step 27

**Files:**
- Create: `D:\AeroCode1\docs\steps\step-27-web-ui-productization.md`

- [ ] Run `scripts\mvn-jdk21.cmd -q -pl axercode-server -am test`.
- [ ] Run `scripts\mvn-jdk21.cmd -q test`.
- [ ] Run `scripts\mvn-jdk21.cmd -q package`.
- [ ] Start the packaged server and confirm `GET /` still returns the page.
- [ ] Write the Step 27 learning summary.

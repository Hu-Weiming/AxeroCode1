# AxerCode Web UI Productization Design

## Goal

Upgrade the first web shell so it feels more like a usable product while staying visually minimal and editorial.

## Scope

This step adds:

- lightweight session continuity across page refreshes
- tool-result rendering in the transcript
- a visible but quiet session-status row
- a `New Session` reset action
- friendlier in-page error feedback

This step does not add:

- a session sidebar
- multi-page navigation
- authentication
- desktop packaging

## Visual Direction

Keep the same white, narrow, quiet reading layout from Step 26.

New UI elements should feel like subtle annotations, not app chrome:

- muted metadata row
- understated text button for reset
- tool feedback as pale bordered notes
- no dark panels, no badges, no dashboard widgets

## Recommended Approach

Continue serving static assets from Spring Boot and reuse existing endpoints:

- `GET /api/sessions/{sessionId}` to rehydrate the transcript
- `POST /api/chat/stream` for prompt streaming

Persist only the `sessionId` in `localStorage`. On load:

- if the id exists, fetch the session
- re-render prior messages
- if loading fails, clear the stale id and fall back to a fresh state

## Design

### 1. Session continuity

Browser state should keep:

- `sessionId`
- current busy state

`sessionId` is mirrored to `localStorage` under one stable key.

### 2. Session status row

Add a quiet meta row between the intro and the transcript showing:

- whether a session is active
- a shortened session id when known
- a `New Session` action

### 3. Tool feedback rendering

When the streaming `complete` payload arrives, render any `toolResults` as a small note block attached to the current assistant reply.

### 4. Error handling

On network or server failure:

- keep the user message in the transcript
- turn the assistant placeholder into a gentle error note
- update the top-level status line

## Files Expected To Change

- `D:\AeroCode1\axercode-server\src\main\resources\static\index.html`
- `D:\AeroCode1\axercode-server\src\main\resources\static\assets\styles.css`
- `D:\AeroCode1\axercode-server\src\main\resources\static\assets\app.js`
- `D:\AeroCode1\axercode-server\src\test\java\com\axercode\server\api\AxerCodeWebUiTest.java`

## Verification Strategy

- static integration tests for the new session-meta shell and updated assets
- full Maven verification for `axercode-server` and the full repo
- packaged server startup plus root-page smoke test
- manual HTTP root verification that the page still serves after productization

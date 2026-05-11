# AxerCode Minimal Web UI Design

## Goal

Add the first browser UI for AxerCode as a minimalist white chat page served directly by `axercode-server`.

## Scope

This step adds:

- a static HTML/CSS/JavaScript page served by Spring Boot
- a minimal conversation layout with strong whitespace and narrow reading width
- browser-side streaming integration with `POST /api/chat/stream`
- local session continuity in the page by storing the returned `sessionId`

This step does not add:

- multi-page navigation
- auth
- session list sidebar
- desktop packaging

## Visual Direction

The page should feel closer to a personal essay page than to a dashboard:

- white background
- dark text
- serif-forward typography
- narrow centered column
- almost no chrome
- subtle borders and muted secondary text

The visual reference is “Naval-style minimalism,” not a developer console.

## Recommended Approach

Serve one static page from `src/main/resources/static`:

- `index.html`
- `assets/styles.css`
- `assets/app.js`

The page will use plain browser APIs:

- `fetch` for streaming POST
- manual SSE parsing from the response body
- progressive message rendering in the transcript

This keeps the first Web UI very small and avoids introducing a frontend framework before the interaction model is stable.

## Design

### 1. Layout

The page should contain:

- a quiet header with “AxerCode”
- a one-line description
- a centered transcript column
- a textarea composer with a single send button

The transcript should show:

- user messages
- assistant messages
- a subtle status row while streaming

### 2. Interaction model

Browser state:

- current `sessionId`
- whether a request is in flight

Behavior:

- submit prompt
- render user bubble immediately
- create an empty assistant bubble
- stream token deltas into that bubble
- on `complete`, update `sessionId` from the final payload
- disable the composer while a request is running

### 3. Styling

Use a restrained type stack and spacing system:

- serif for headings and long-form feel
- sans-serif for controls if needed
- off-black text on white background
- thin gray borders
- minimal hover states

Avoid:

- gradients
- heavy shadows
- bright accent colors
- sidebar-heavy app chrome

## Files Expected To Change

- `D:\AeroCode1\axercode-server\src\main\resources\static\index.html`
- `D:\AeroCode1\axercode-server\src\main\resources\static\assets\styles.css`
- `D:\AeroCode1\axercode-server\src\main\resources\static\assets\app.js`
- `D:\AeroCode1\axercode-server\src\test\java\com\axercode\server\api\AxerCodeWebUiTest.java`

## Verification Strategy

- integration test for `GET /` returning the web shell
- integration test for the static CSS and JS assets being served
- full module and repository Maven verification
- packaged server startup plus manual browser-facing smoke check through `GET /`

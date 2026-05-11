# AxerCode Focus-Aware Prompting Design

## Goal

Make the persisted `/focus` path actually affect model turns by injecting it into the provider request context, while keeping persisted conversation history clean.

## Scope

This step covers:

- reading the current focus path from `ShellStateStore`
- turning that focus into a temporary `SYSTEM` message
- including that system message in agent/provider requests
- ensuring the temporary focus message does not get persisted into `SessionStore`

This step does **not** cover:

- automatic file loading from the focus path
- plan mode
- branch-aware focus
- Web or desktop focus surfaces

## Problem

Step 15 made `/focus` durable across restarts, but it is still only shell metadata.

Users can see it in `/status`, but it does not yet influence:

- how the model resolves relative workspace references
- how the model interprets the current working context

That means focus exists, but it is not yet useful to the model.

## Approaches Considered

### 1. Concatenate focus text directly into the user prompt

Fastest to ship, but it pollutes user input and makes later context features harder to separate cleanly.

### 2. Add temporary system-context augmentation before the agent call

Keeps user input clean and creates a reusable path for future context features such as plan mode or branch metadata.

### 3. Persist focus as a permanent `SYSTEM` message in the session history

Simple to implement, but it dirties the persisted conversation and risks repeated focus messages over time.

## Recommended Design

Use approach 2.

Add a CLI-side context augmenter that:

1. reads the current focus path from `ShellStateStore`
2. builds a temporary `SYSTEM` message when focus exists
3. prepends it to the session only for the current agent turn
4. strips it back out before the updated session is returned to `SessionStore`

## Message Shape

The injected system message should be concise and operational, for example:

`Current focus path: <path>. Prefer this path when resolving relative project references for this turn.`

It should guide the model without pretending files have already been read.

## Why This Design

The key design choice is making focus:

- visible to the model
- invisible to persisted session history

That keeps the shell state useful without contaminating long-term conversation storage.

It also creates a clean extension point for future ephemeral context:

- plan mode status
- active branch metadata
- checkpoint labels

## Testing Strategy

Add tests for:

- `CliChatService` injecting a focus system message when focus exists
- the returned session not persisting the temporary system message
- interactive shell focus commands influencing later prompt turns through the same shared store

## Outcome

After this step, `/focus` will stop being passive metadata and start shaping model behavior on every turn, while the persisted session remains clean.

# AxerCode Step 15 Persistent Shell State Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist interactive shell focus and named checkpoints through SQLite so advanced shell workflows survive CLI restarts.

**Architecture:** Introduce a `ShellStateStore` abstraction in the CLI module, backed by an in-memory implementation for tests and a SQLite-backed implementation for production. Add a `SqliteShellStateRepository` in the storage module and let it reuse `SqliteSessionRepository` for checkpoint session snapshots.

**Tech Stack:** Java 21, Spring Boot 3.3.4, SQLite JDBC, JUnit 5

---

### Task 1: Lock persistence behavior with failing tests

**Files:**
- Create: `axercode-storage-sqlite/src/test/java/com/axercode/storage/sqlite/shell/SqliteShellStateRepositoryTest.java`
- Create: `axercode-cli/src/test/java/com/axercode/cli/service/SqliteBackedShellStateStoreTest.java`
- Modify: `axercode-cli/src/test/java/com/axercode/cli/service/InteractiveShellServiceTest.java`

- [ ] **Step 1: Add a failing repository test for persisting and reloading focus path**
- [ ] **Step 2: Add a failing repository test for persisting and reloading named checkpoints**
- [ ] **Step 3: Add a failing CLI integration test that uses two shell instances to verify focus and checkpoints survive restart**
- [ ] **Step 4: Run the targeted tests and confirm they fail because the repository and store types do not exist yet**

### Task 2: Add shell-state persistence boundaries

**Files:**
- Create: `axercode-cli/src/main/java/com/axercode/cli/service/ShellStateStore.java`
- Create: `axercode-cli/src/main/java/com/axercode/cli/service/InMemoryShellStateStore.java`
- Create: `axercode-cli/src/main/java/com/axercode/cli/service/SqliteBackedShellStateStore.java`
- Create: `axercode-storage-sqlite/src/main/java/com/axercode/storage/sqlite/shell/SqliteShellStateRepository.java`
- Modify: `axercode-storage-sqlite/src/main/java/com/axercode/storage/sqlite/session/SqliteSessionRepository.java`
- Modify: `axercode-cli/src/main/java/com/axercode/cli/config/StorageConfiguration.java`

- [ ] **Step 1: Add a reusable `saveSession(SessionContext)` entry point to `SqliteSessionRepository`**
- [ ] **Step 2: Implement `SqliteShellStateRepository` with focus and checkpoint tables**
- [ ] **Step 3: Add `ShellStateStore` plus in-memory and SQLite-backed implementations**
- [ ] **Step 4: Wire the SQLite-backed shell-state store in `StorageConfiguration`**
- [ ] **Step 5: Re-run targeted repository and shell-state store tests and confirm they pass**

### Task 3: Integrate persistent shell state into slash commands

**Files:**
- Modify: `axercode-cli/src/main/java/com/axercode/cli/shell/SlashCommandDispatcher.java`
- Modify: `axercode-cli/src/main/java/com/axercode/cli/service/InteractiveShellService.java`
- Modify: `axercode-cli/src/main/java/com/axercode/cli/shell/ShellRuntimeState.java`

- [ ] **Step 1: Move focus and checkpoint persistence behavior from transient runtime state to `ShellStateStore`**
- [ ] **Step 2: Keep `ShellRuntimeState` only for truly per-run state, or remove it if it becomes unnecessary**
- [ ] **Step 3: Re-run interactive shell tests and confirm persisted commands still behave the same**

### Task 4: Verify and document the step

**Files:**
- Create: `docs/steps/step-15-persistent-shell-state.md`

- [ ] **Step 1: Run `axercode-storage-sqlite` tests**
- [ ] **Step 2: Run `axercode-cli` tests**
- [ ] **Step 3: Run the full reactor tests**
- [ ] **Step 4: Package the project**
- [ ] **Step 5: Run a restart-style interactive verification against one SQLite database**
- [ ] **Step 6: Write the learning summary and Step 15 result block**

# AxerCode Step 9 SQLite Session Persistence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist the current CLI conversation session to SQLite so AxerCode can restore message history across process restarts.

**Architecture:** Add a plain JDBC-based SQLite repository in `axercode-storage-sqlite`, then wire a CLI `SessionStore` implementation that loads the current session on startup and saves it on every shell update. Keep the existing in-memory session store as a lightweight test double so the shell tests stay fast and deterministic.

**Tech Stack:** Java 21, Spring Boot 3.3.4, SQLite JDBC, JUnit 5

---

### Task 1: Add SQLite storage tests and repository skeleton

**Files:**
- Modify: `axercode-storage-sqlite/pom.xml`
- Create: `axercode-storage-sqlite/src/test/java/com/axercode/storage/sqlite/session/SqliteSessionRepositoryTest.java`
- Create: `axercode-storage-sqlite/src/main/java/com/axercode/storage/sqlite/session/SqliteSessionRepository.java`

- [ ] **Step 1: Write failing tests for saving and loading the current session from SQLite**
- [ ] **Step 2: Write a failing test for switching the active session while preserving message order**
- [ ] **Step 3: Run the targeted storage tests and verify they fail because the repository does not exist**
- [ ] **Step 4: Implement the minimal SQLite repository and schema bootstrap**
- [ ] **Step 5: Re-run the storage tests and verify they pass**

### Task 2: Add a CLI session-store abstraction and persistent implementation

**Files:**
- Create: `axercode-cli/src/main/java/com/axercode/cli/service/SessionStore.java`
- Modify: `axercode-cli/src/main/java/com/axercode/cli/service/InMemorySessionStore.java`
- Create: `axercode-cli/src/main/java/com/axercode/cli/service/SqliteBackedSessionStore.java`
- Create: `axercode-cli/src/test/java/com/axercode/cli/service/SqliteBackedSessionStoreTest.java`

- [ ] **Step 1: Write failing tests for loading a persisted current session into the CLI session store**
- [ ] **Step 2: Write a failing test for persisting updates and resets through the CLI session store**
- [ ] **Step 3: Run the targeted CLI storage tests and verify they fail**
- [ ] **Step 4: Implement the `SessionStore` abstraction and SQLite-backed implementation**
- [ ] **Step 5: Keep `InMemorySessionStore` as the non-Spring test implementation**
- [ ] **Step 6: Re-run the targeted CLI storage tests and verify they pass**

### Task 3: Wire SQLite persistence into the Spring Boot CLI

**Files:**
- Create: `axercode-cli/src/main/java/com/axercode/cli/config/AxerCodeStorageProperties.java`
- Create: `axercode-cli/src/main/java/com/axercode/cli/config/StorageConfiguration.java`
- Modify: `axercode-cli/src/main/java/com/axercode/cli/bootstrap/AxerCodeCliApplication.java`
- Modify: `axercode-cli/src/main/java/com/axercode/cli/service/InteractiveShellService.java`
- Modify: `axercode-cli/src/main/resources/application.yml`
- Modify: `axercode-cli/src/test/java/com/axercode/cli/service/InteractiveShellServiceTest.java`

- [ ] **Step 1: Add a configurable SQLite database file path for the CLI**
- [ ] **Step 2: Register the SQLite repository bean in the CLI Spring configuration**
- [ ] **Step 3: Switch `InteractiveShellService` to depend on the `SessionStore` abstraction**
- [ ] **Step 4: Update the shell tests to use the in-memory implementation directly**
- [ ] **Step 5: Re-run the targeted shell tests and verify they still pass**

### Task 4: Verify real persistence and document the step

**Files:**
- Create: `docs/steps/step-9-sqlite-session-persistence.md`

- [ ] **Step 1: Run `axercode-storage-sqlite` tests**
- [ ] **Step 2: Run `axercode-cli` tests**
- [ ] **Step 3: Run the full reactor tests**
- [ ] **Step 4: Package the CLI jar**
- [ ] **Step 5: Run the CLI twice against the same SQLite file and verify the second run restores history**
- [ ] **Step 6: Write the learning summary and Step 9 result block**

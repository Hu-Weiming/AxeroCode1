# AxerCode Sidebar Collapse And Model Labels Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a collapsible desktop history sidebar, restyle the sidebar into the same light grayscale family as the main panel, and show the actual assistant model name on each assistant message.

**Architecture:** Keep the existing static web UI and server APIs, but extend the session payload so assistant messages can carry per-message model metadata loaded from SQLite. Preserve historical assistant model names during future saves by storing them separately from the core message body, then update the frontend to render that metadata and to manage a persistent desktop sidebar-collapsed state.

**Tech Stack:** Spring Boot, static HTML/CSS/JavaScript, SQLite/JDBC, JUnit 5, MockMvc

---

### Task 1: Lock In The New UI And API Surface With Failing Tests

**Files:**
- Modify: `D:/AeroCode1/axercode-server/src/test/java/com/axercode/server/api/AxerCodeWebUiTest.java`
- Modify: `D:/AeroCode1/axercode-server/src/test/java/com/axercode/server/api/AxerCodeChatControllerTest.java`
- Modify: `D:/AeroCode1/axercode-server/src/test/java/com/axercode/server/service/ServerConversationServiceTest.java`
- Modify: `D:/AeroCode1/axercode-storage-sqlite/src/test/java/com/axercode/storage/sqlite/session/SqliteSessionRepositoryTest.java`

- [ ] **Step 1: Add UI assertions for the desktop collapse affordance and light sidebar styling**

```java
.andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"sidebarCollapseButton\"")))
.andExpect(content().string(org.hamcrest.Matchers.containsString("sidebar-collapsed")))
.andExpect(content().string(org.hamcrest.Matchers.containsString("--sidebar-bg: #f3f4f6;")))
```

- [ ] **Step 2: Add API assertions for assistant message model metadata**

```java
.andExpect(jsonPath("$.messages[1].role").value("ASSISTANT"))
.andExpect(jsonPath("$.messages[1].model").value("claude-3-5-sonnet-latest"))
```

- [ ] **Step 3: Add persistence assertions showing old assistant messages keep their original model after later turns switch models**

```java
assertEquals("claude-3-5-sonnet-latest", loaded.messageModels().get(firstAssistant.id()));
assertEquals("qwen2.5:14b", loaded.messageModels().get(secondAssistant.id()));
```

- [ ] **Step 4: Run the focused tests and confirm they fail for the new expectations**

Run:
`D:\\AeroCode1\\scripts\\mvn-jdk21.cmd -q -pl axercode-storage-sqlite,axercode-server -am test -Dtest=SqliteSessionRepositoryTest,ServerConversationServiceTest,AxerCodeChatControllerTest,AxerCodeWebUiTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: FAIL because the sidebar collapse button, light sidebar styling, and per-message assistant model metadata do not exist yet.

### Task 2: Persist And Expose Per-Assistant Model Metadata

**Files:**
- Modify: `D:/AeroCode1/axercode-storage-sqlite/src/main/java/com/axercode/storage/sqlite/session/SqliteSessionRepository.java`
- Modify: `D:/AeroCode1/axercode-server/src/main/java/com/axercode/server/service/ServerStoredSession.java`
- Modify: `D:/AeroCode1/axercode-server/src/main/java/com/axercode/server/service/ServerConversationService.java`
- Modify: `D:/AeroCode1/axercode-server/src/main/java/com/axercode/server/api/SessionMessageResponse.java`

- [ ] **Step 1: Extend storage to keep assistant-message model mappings in a separate SQLite table**

```sql
create table if not exists session_message_models (
    message_id text primary key,
    session_id text not null,
    model_name text not null,
    foreign key (session_id) references sessions(session_id),
    foreign key (message_id) references session_messages(message_id)
)
```

- [ ] **Step 2: Preserve existing mappings during save and assign the current model only to newly-added assistant messages**

```java
String preservedModel = existingMessageModels.get(message.id());
if (preservedModel != null && !preservedModel.isBlank()) {
    resolvedModels.put(message.id(), preservedModel);
} else if (message.role() == MessageRole.ASSISTANT && currentModelName != null && !currentModelName.isBlank()) {
    resolvedModels.put(message.id(), currentModelName);
}
```

- [ ] **Step 3: Surface the mapping through the server session view and API response**

```java
public record SessionMessageResponse(
        String role,
        String content,
        Instant createdAt,
        String model
) { }
```

- [ ] **Step 4: Re-run the focused backend tests until they pass**

Run:
`D:\\AeroCode1\\scripts\\mvn-jdk21.cmd -q -pl axercode-storage-sqlite,axercode-server -am test -Dtest=SqliteSessionRepositoryTest,ServerConversationServiceTest,AxerCodeChatControllerTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS

### Task 3: Implement Sidebar Collapse, Light Sidebar Styling, And Model-Based Labels

**Files:**
- Modify: `D:/AeroCode1/axercode-server/src/main/resources/static/index.html`
- Modify: `D:/AeroCode1/axercode-server/src/main/resources/static/assets/styles.css`
- Modify: `D:/AeroCode1/axercode-server/src/main/resources/static/assets/app.js`

- [ ] **Step 1: Add the sidebar collapse button and desktop-collapsed shell state hooks in the markup**

```html
<button type="button" class="sidebar-collapse-button" id="sidebarCollapseButton" aria-label="Collapse history">Collapse</button>
```

- [ ] **Step 2: Restyle the sidebar into a light grayscale panel and add the collapsed desktop layout**

```css
.app-shell.sidebar-collapsed .sidebar {
    display: none;
}

.app-shell.sidebar-collapsed .sidebar-toggle-button {
    display: inline-flex;
}
```

- [ ] **Step 3: Use the real model name for assistant labels in both streaming and restored history**

```javascript
const assistantView = appendMessage('assistant', assistantLabel(currentModel()), '');
setMessageRoleLabel(assistantView, assistantLabel(payload.model ?? currentModel()));
```

- [ ] **Step 4: Re-run the focused web UI tests**

Run:
`D:\\AeroCode1\\scripts\\mvn-jdk21.cmd -q -pl axercode-server -am test -Dtest=AxerCodeWebUiTest -Dsurefire.failIfNoSpecifiedTests=false`

Expected: PASS

### Task 4: Full Verification And Local Preview

**Files:**
- No additional file edits expected

- [ ] **Step 1: Run the combined server and storage test suites**

Run:
`D:\\AeroCode1\\scripts\\mvn-jdk21.cmd -q -pl axercode-storage-sqlite,axercode-server -am test`

Expected: PASS

- [ ] **Step 2: Restart the local preview so the user can validate the updated UI**

Run:
`powershell -ExecutionPolicy Bypass -File D:\\AeroCode1\\scripts\\launch-desktop-preview.ps1`

Expected: the local web preview starts successfully on `http://127.0.0.1:19090/`

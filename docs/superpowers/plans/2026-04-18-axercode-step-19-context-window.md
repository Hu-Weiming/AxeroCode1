# Step 19 Context Window Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a role-aware sliding window that trims provider request history while preserving full conversation persistence.

**Architecture:** Introduce a small core session-window utility, thread it into `ToolCallingAgent`, and configure the limits from the CLI Spring runtime. Provider requests become bounded, but `SessionContext` and SQLite still store the full history.

**Tech Stack:** Java 21, Spring Boot 3.3.4, JUnit 5, Picocli, JLine 3

---

### Task 1: Add the core session window utility

**Files:**
- Create: `D:\AeroCode1\axercode-core\src\main\java\com\axercode\core\session\SessionContextWindow.java`
- Create: `D:\AeroCode1\axercode-core\src\test\java\com\axercode\core\session\SessionContextWindowTest.java`

- [ ] Write failing tests for no-op behavior, system-message preservation, and recent non-system trimming.
- [ ] Run `scripts\mvn-jdk21.cmd -q -pl axercode-core -am test "-Dtest=SessionContextWindowTest" "-Dsurefire.failIfNoSpecifiedTests=false"` and confirm red.
- [ ] Implement the minimal `SessionContextWindow`.
- [ ] Re-run the same command and confirm green.

### Task 2: Apply the window in the agent loop

**Files:**
- Modify: `D:\AeroCode1\axercode-agent\src\main\java\com\axercode\agent\ToolCallingAgent.java`
- Modify: `D:\AeroCode1\axercode-agent\src\test\java\com\axercode\agent\ToolCallingAgentTest.java`

- [ ] Add failing tests proving provider requests are trimmed while final session history remains complete.
- [ ] Run `scripts\mvn-jdk21.cmd -q -pl axercode-agent -am test "-Dtest=ToolCallingAgentTest" "-Dsurefire.failIfNoSpecifiedTests=false"` and confirm red.
- [ ] Implement request-time trimming in `ToolCallingAgent`.
- [ ] Re-run the same command and confirm green.

### Task 3: Externalize agent limits in CLI configuration

**Files:**
- Create: `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\config\AxerCodeAgentProperties.java`
- Modify: `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\config\ToolConfiguration.java`
- Modify: `D:\AeroCode1\axercode-cli\src\main\resources\application.yml`

- [ ] Add a failing Spring wiring test if the new properties change bean construction behavior.
- [ ] Run the targeted CLI test command and confirm red if a new test is added.
- [ ] Implement configurable `maxToolRounds` and `maxRecentMessages`.
- [ ] Re-run CLI tests and confirm green.

### Task 4: Verify and document Step 19

**Files:**
- Create: `D:\AeroCode1\docs\steps\step-19-context-window.md`

- [ ] Run `scripts\mvn-jdk21.cmd -q -pl axercode-core -am test`.
- [ ] Run `scripts\mvn-jdk21.cmd -q -pl axercode-agent -am test`.
- [ ] Run `scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test`.
- [ ] Run `scripts\mvn-jdk21.cmd -q test`.
- [ ] Run `scripts\mvn-jdk21.cmd -q package`.
- [ ] Run the packaged CLI on JDK 21 and confirm normal prompt execution still works with the new window.
- [ ] Write the Step 19 learning summary.

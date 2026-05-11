# Step 21 Reflection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a bounded reflection/self-correction prompt after failed tool rounds.

**Architecture:** Keep failed tool observations in the session, but inject a temporary reflection `SYSTEM` message into the following provider request. Track a small per-turn reflection budget and configure it from the CLI runtime.

**Tech Stack:** Java 21, Spring Boot 3.3.4, JUnit 5

---

### Task 1: Add failing agent tests for reflection behavior

**Files:**
- Modify: `D:\AeroCode1\axercode-agent\src\test\java\com\axercode\agent\ToolCallingAgentTest.java`

- [ ] Write a failing test proving the request after a failed tool round contains a reflection system message.
- [ ] Write a failing test proving the reflection message stops appearing after the configured reflection limit is exhausted.
- [ ] Run `scripts\mvn-jdk21.cmd -q -pl axercode-agent -am test "-Dtest=ToolCallingAgentTest" "-Dsurefire.failIfNoSpecifiedTests=false"` and confirm red.

### Task 2: Implement bounded reflection in the agent loop

**Files:**
- Modify: `D:\AeroCode1\axercode-agent\src\main\java\com\axercode\agent\ToolCallingAgent.java`

- [ ] Add the minimal reflection-budget state to the agent loop.
- [ ] Inject a temporary reflection system message only into the next provider request after a failed tool round.
- [ ] Re-run the targeted agent test command and confirm green.

### Task 3: Expose reflection budget in CLI config

**Files:**
- Modify: `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\config\AxerCodeAgentProperties.java`
- Modify: `D:\AeroCode1\axercode-cli\src\main\java\com\axercode\cli\config\ToolConfiguration.java`
- Modify: `D:\AeroCode1\axercode-cli\src\main\resources\application.yml`

- [ ] Add the new property to the runtime configuration.
- [ ] Wire it into the `ToolCallingAgent` bean construction.
- [ ] Run `scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test` and confirm green.

### Task 4: Verify and document Step 21

**Files:**
- Create: `D:\AeroCode1\docs\steps\step-21-reflection.md`

- [ ] Run `scripts\mvn-jdk21.cmd -q -pl axercode-agent -am test`.
- [ ] Run `scripts\mvn-jdk21.cmd -q -pl axercode-cli -am test`.
- [ ] Run `scripts\mvn-jdk21.cmd -q test`.
- [ ] Run `scripts\mvn-jdk21.cmd -q package`.
- [ ] Run the packaged CLI on JDK 21 and confirm normal prompt execution still works.
- [ ] Write the Step 21 learning summary.

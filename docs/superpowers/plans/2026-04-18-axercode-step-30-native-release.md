# Native Release Pipeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish the AxerCode release pipeline by wiring GraalVM Native Image support into Maven, adding D-drive GraalVM install helpers, and verifying AOT-on-JVM for both CLI and server on the current machine.

**Architecture:** Keep the current JVM and `jpackage` flows intact while adding AOT/native build tracks beside them. Use scripts to separate what can be verified now from what is blocked by missing GraalVM or Windows linker prerequisites.

**Tech Stack:** Java 21, Spring Boot 3.3.4, GraalVM Native Build Tools, Spring AOT, Windows CMD/PowerShell scripts

---

### Task 1: Add native release docs and build properties

**Files:**
- Modify: `D:/AeroCode1/pom.xml`
- Create: `D:/AeroCode1/docs/superpowers/specs/2026-04-18-axercode-native-release-design.md`
- Create: `D:/AeroCode1/docs/superpowers/plans/2026-04-18-axercode-step-30-native-release.md`

- [ ] Add the shared Graal native build tools version and plugin management.
- [ ] Save the Step 30 design and plan docs.

### Task 2: Wire CLI and server AOT/native build profiles

**Files:**
- Modify: `D:/AeroCode1/axercode-cli/pom.xml`
- Modify: `D:/AeroCode1/axercode-server/pom.xml`

- [ ] Add Spring Boot `process-aot` support for CLI and server.
- [ ] Add Graal native build plugin wiring for CLI and server.
- [ ] Keep the default JVM packaging flow unchanged.

### Task 3: Add GraalVM/native helper scripts

**Files:**
- Create: `D:/AeroCode1/scripts/install-graalvm-on-d.cmd`
- Create: `D:/AeroCode1/scripts/install-graalvm-on-d.ps1`
- Create: `D:/AeroCode1/scripts/build-native-cli.cmd`
- Create: `D:/AeroCode1/scripts/build-native-cli.ps1`
- Create: `D:/AeroCode1/scripts/build-native-server.cmd`
- Create: `D:/AeroCode1/scripts/build-native-server.ps1`
- Create: `D:/AeroCode1/scripts/verify-cli-aot-jvm.cmd`
- Create: `D:/AeroCode1/scripts/verify-cli-aot-jvm.ps1`
- Create: `D:/AeroCode1/scripts/verify-server-aot-jvm.cmd`
- Create: `D:/AeroCode1/scripts/verify-server-aot-jvm.ps1`
- Modify: `D:/AeroCode1/README.md`

- [ ] Add D-drive GraalVM helper scripts.
- [ ] Add friendly native build preflight scripts.
- [ ] Add AOT-on-JVM verification scripts for CLI and server.
- [ ] Document the native release path in the README.

### Task 4: Run final verification and document Step 30

**Files:**
- Create: `D:/AeroCode1/docs/steps/step-30-native-release.md`

- [ ] Run sequential repository verification.
- [ ] Run CLI AOT-on-JVM verification.
- [ ] Run server AOT-on-JVM verification.
- [ ] Run native preflight scripts and capture the real blocker status.
- [ ] Write the final Step 30 summary with exact completion status.

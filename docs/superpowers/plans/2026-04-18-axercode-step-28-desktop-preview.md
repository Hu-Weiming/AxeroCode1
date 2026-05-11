# Desktop Preview Runtime Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Windows-friendly desktop preview mode that launches the local Spring Boot server in a dedicated desktop profile and opens the AxerCode web UI automatically.

**Architecture:** Keep `axercode-server` as the single runtime, add a desktop-only startup layer around it, and use Windows launcher scripts for the preview experience. Later native packaging can wrap this already-working runtime instead of redefining startup behavior.

**Tech Stack:** Java 21, Spring Boot 3.3.4, JUnit 5, Mockito, Windows CMD/PowerShell scripts

---

### Task 1: Define desktop preview behavior with failing tests

**Files:**
- Create: `D:/AeroCode1/axercode-server/src/test/java/com/axercode/server/desktop/DesktopUrlBuilderTest.java`
- Create: `D:/AeroCode1/axercode-server/src/test/java/com/axercode/server/desktop/DesktopStartupLauncherTest.java`
- Create: `D:/AeroCode1/axercode-server/src/test/java/com/axercode/server/desktop/DesktopApplicationReadyListenerTest.java`

- [ ] Write failing tests for URL normalization, enable/disable launch behavior, and application-ready forwarding.
- [ ] Run targeted desktop tests and confirm they fail for missing classes.

### Task 2: Implement desktop preview runtime classes

**Files:**
- Create: `D:/AeroCode1/axercode-server/src/main/java/com/axercode/server/desktop/DesktopApplicationProperties.java`
- Create: `D:/AeroCode1/axercode-server/src/main/java/com/axercode/server/desktop/DesktopUrlBuilder.java`
- Create: `D:/AeroCode1/axercode-server/src/main/java/com/axercode/server/desktop/BrowserLauncher.java`
- Create: `D:/AeroCode1/axercode-server/src/main/java/com/axercode/server/desktop/SystemBrowserLauncher.java`
- Create: `D:/AeroCode1/axercode-server/src/main/java/com/axercode/server/desktop/DesktopStartupLauncher.java`
- Create: `D:/AeroCode1/axercode-server/src/main/java/com/axercode/server/desktop/DesktopApplicationReadyListener.java`
- Modify: `D:/AeroCode1/axercode-server/src/main/java/com/axercode/server/bootstrap/AxerCodeServerApplication.java`

- [ ] Implement the minimal classes required to make the tests pass.
- [ ] Register desktop properties with the Spring Boot app.
- [ ] Re-run targeted desktop tests and confirm they pass.

### Task 3: Add desktop profile config and launcher scripts

**Files:**
- Create: `D:/AeroCode1/axercode-server/src/main/resources/application-desktop.yml`
- Create: `D:/AeroCode1/scripts/launch-desktop-preview.cmd`
- Create: `D:/AeroCode1/scripts/launch-desktop-preview.ps1`

- [ ] Add desktop-safe localhost profile defaults.
- [ ] Add Windows launcher scripts that build and run the packaged server with the desktop profile.
- [ ] Re-run `axercode-server` module tests.

### Task 4: Verify packaged desktop preview and document Step 28

**Files:**
- Create: `D:/AeroCode1/docs/steps/step-28-desktop-preview.md`

- [ ] Run sequential repository verification with `scripts/mvn-jdk21.cmd`.
- [ ] Launch the packaged server in desktop profile and verify the root UI is served.
- [ ] Write the Step 28 learning summary with behavior, verification, and follow-on notes.

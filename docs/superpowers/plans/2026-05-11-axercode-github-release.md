# AxerCode GitHub Release Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publish the AxerCode source tree to GitHub with safe defaults, clear setup documentation, and four ready-to-run Windows artifacts checked into the repository.

**Architecture:** Keep the repository source-first, but preserve the user-requested `AAAstart/` release bundle as a tracked directory. Document runtime prerequisites, configuration touchpoints, and the exact scripts needed to rebuild each delivery format.

**Tech Stack:** Git, GitHub, Maven, Spring Boot, GraalVM native-image, jpackage, Rust/Cargo, Node.js, Markdown documentation

---

### Task 1: Define the public repository boundary

**Files:**
- Modify: `.gitignore`
- Review: `AAAstart/`, `tmp/`, `dist/`, `desktop/pake-shell/node_modules/`

- [ ] Exclude local-only state such as temp databases, logs, and IDE files.
- [ ] Keep `AAAstart/` tracked because it contains the four distributable Windows builds the user wants to publish.
- [ ] Keep source modules, scripts, and docs tracked.

### Task 2: Replace the root README with a public-facing overview

**Files:**
- Modify: `README.md`

- [ ] Document what AxerCode is, who it is for, and the Windows-first workflow.
- [ ] Summarize the multi-module architecture and the four end-user delivery formats.
- [ ] Provide quick-start instructions for Ollama, CLI, web, and desktop preview usage.
- [ ] Link to the deeper setup guide and note the current license status honestly.

### Task 3: Add a detailed setup and release guide

**Files:**
- Create: `docs/getting-started.md`
- Modify: `desktop/pake-shell/README.md`

- [ ] Explain clone-time prerequisites: JDK 21, Maven 3.9+, Ollama, and optional cloud-provider keys.
- [ ] List which configuration values a new user may need to change and where they live.
- [ ] Document the exact scripts for native CLI, native server, jpackage desktop, and Pake shell builds.
- [ ] Refresh the Pake sub-readme so it matches the current machine/toolchain expectations.

### Task 4: Rebuild and refresh the tracked release artifacts

**Files:**
- Refresh output under: `AAAstart/axercode-cli.exe/`
- Refresh output under: `AAAstart/axercode-server.exe/`
- Refresh output under: `AAAstart/AxerCode.exe/`
- Refresh output under: `AAAstart/AxerCode-Pake.exe/`

- [ ] Run the native CLI build and capture the fresh executable plus companion SQLite DLL.
- [ ] Run the native server build and capture the fresh executable plus companion SQLite DLL.
- [ ] Run the jpackage desktop build and refresh the tracked app-image directory.
- [ ] Run the Pake build and refresh the tracked desktop shell executable.

### Task 5: Verify, commit, and publish

**Files:**
- Review: full repository diff

- [ ] Run the relevant Maven test suite after doc and packaging updates.
- [ ] Verify the four artifact folders exist and contain the expected binaries.
- [ ] Configure the GitHub remote if missing, create the first commit, and push to `https://github.com/Hu-Weiming/AxeroCode1.git`.
- [ ] Report any remaining publication risks, especially the missing open-source license if one is still not chosen.

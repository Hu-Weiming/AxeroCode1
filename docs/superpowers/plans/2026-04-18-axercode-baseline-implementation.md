# AxerCode Baseline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Freeze the local Windows development baseline for AxerCode before any Java module or feature code is created.

**Architecture:** This milestone establishes deterministic project constraints rather than business logic. It locks toolchain versions, records the selected architecture, and adds helper scripts so every later Maven and Java command runs under JDK 21 instead of the machine-default Java 8 path.

**Tech Stack:** Windows 11, JDK 21, Maven 3.9.12, Ollama, Git, Markdown, CMD helper scripts

---

### Task 1: Record the baseline decisions

**Files:**
- Create: `README.md`
- Create: `docs/superpowers/specs/2026-04-18-axercode-design.md`

- [ ] **Step 1: Write the baseline repository overview**

```md
# AxerCode

AxerCode is a locally hosted AI coding assistant project targeting a Claude Code / Codex style workflow on Windows first.
```

- [ ] **Step 2: Write the architectural baseline document**

```md
## Recommended Architecture

Use a unified domain core with thin delivery shells.
```

- [ ] **Step 3: Verify the documents exist**

Run: `Get-ChildItem README.md, docs\superpowers\specs\2026-04-18-axercode-design.md`
Expected: both files are listed without errors

- [ ] **Step 4: Commit**

```bash
git add README.md docs/superpowers/specs/2026-04-18-axercode-design.md
git commit -m "docs: add AxerCode baseline design"
```

### Task 2: Freeze JDK 21 and Maven execution

**Files:**
- Create: `scripts/mvn-jdk21.cmd`
- Create: `scripts/verify-baseline.cmd`

- [ ] **Step 1: Write the Maven JDK 21 wrapper**

```bat
@echo off
setlocal
set "JAVA_HOME=C:\Program Files\Java\jdk-21"
set "PATH=%JAVA_HOME%\bin;%PATH%"
call "C:\Users\胡炜铭\.m2\wrapper\dists\apache-maven-3.9.12-bin\5nmfsn99br87k5d4ajlekdq10k\apache-maven-3.9.12\bin\mvn.cmd" %*
exit /b %ERRORLEVEL%
```

- [ ] **Step 2: Write the baseline verification script**

```bat
@echo off
setlocal
set "EXPECTED_MODEL=qwen2.5:7b"
ollama list | findstr /C:"%EXPECTED_MODEL%" >nul
if errorlevel 1 exit /b 1
echo [AxerCode] Baseline verification passed.
```

- [ ] **Step 3: Run the verification wrapper**

Run: `scripts\verify-baseline.cmd`
Expected: Java 21, Maven 3.9.12, and `qwen2.5:7b` verification complete successfully

- [ ] **Step 4: Commit**

```bash
git add scripts/mvn-jdk21.cmd scripts/verify-baseline.cmd
git commit -m "build: lock local JDK21 and Maven baseline"
```

### Task 3: Add step-level learning notes

**Files:**
- Create: `docs/steps/step-1-environment-baseline.md`

- [ ] **Step 1: Write the step summary document**

```md
## Step 1 Goal

Freeze the local execution baseline before Java implementation starts.
```

- [ ] **Step 2: Verify the summary file exists**

Run: `Get-ChildItem docs\steps\step-1-environment-baseline.md`
Expected: the file is listed without errors

- [ ] **Step 3: Commit**

```bash
git add docs/steps/step-1-environment-baseline.md
git commit -m "docs: record step 1 learning notes"
```

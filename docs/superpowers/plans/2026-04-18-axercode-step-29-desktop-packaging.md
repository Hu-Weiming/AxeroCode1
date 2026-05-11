# Desktop Packaging Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the first real Windows desktop packaging layer for AxerCode, using a working `jpackage` app-image now and a repo-tracked Pake scaffold for later native shell builds.

**Architecture:** Reuse the existing `axercode-server` desktop profile as the packaged backend runtime. Use JDK 21 `jpackage` for the immediately verifiable app-image, and keep the Pake route alive through a local Node workspace plus Rust-on-D helper scripts.

**Tech Stack:** Java 21, Spring Boot 3.3.4, jpackage, Node.js 24, npm, Pake CLI scaffold, Windows CMD/PowerShell scripts

---

### Task 1: Define desktop packaging files and repo hygiene

**Files:**
- Modify: `D:/AeroCode1/.gitignore`
- Create: `D:/AeroCode1/docs/superpowers/specs/2026-04-18-axercode-desktop-packaging-design.md`
- Create: `D:/AeroCode1/docs/superpowers/plans/2026-04-18-axercode-step-29-desktop-packaging.md`

- [ ] Add ignore rules for desktop build outputs.
- [ ] Write the Step 29 design and plan docs.

### Task 2: Add Pake workspace scaffold

**Files:**
- Create: `D:/AeroCode1/desktop/pake-shell/package.json`
- Create: `D:/AeroCode1/desktop/pake-shell/README.md`
- Create: `D:/AeroCode1/scripts/build-pake-shell.cmd`
- Create: `D:/AeroCode1/scripts/build-pake-shell.ps1`
- Create: `D:/AeroCode1/scripts/install-rust-on-d.cmd`
- Create: `D:/AeroCode1/scripts/install-rust-on-d.ps1`

- [ ] Add a pinned `pake-cli` workspace.
- [ ] Add friendly helper scripts for the Pake route and D-drive Rust installation.
- [ ] Run `npm install` in the Pake workspace to verify the Node side is sound.

### Task 3: Add working jpackage app-image builder

**Files:**
- Create: `D:/AeroCode1/scripts/build-desktop-app-image.cmd`
- Create: `D:/AeroCode1/scripts/build-desktop-app-image.ps1`
- Modify: `D:/AeroCode1/README.md`

- [ ] Add a Windows app-image build script around `jpackage`.
- [ ] Document the working desktop packaging path in the README.
- [ ] Run the app-image build and verify the packaged executable exists.

### Task 4: Validate packaged runtime and document Step 29

**Files:**
- Create: `D:/AeroCode1/docs/steps/step-29-desktop-packaging.md`

- [ ] Launch the packaged desktop app with browser auto-open disabled for verification.
- [ ] Probe the local root UI and confirm the packaged app serves it.
- [ ] Write the Step 29 summary with the exact status of the Pake route and current blockers.

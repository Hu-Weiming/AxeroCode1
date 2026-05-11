# Step 1 - Environment Baseline and Project Freeze

## Step Goal

Before creating any Java code, freeze the local execution baseline so the new AxerCode project is reproducible on this machine.

## What Was Done

1. Confirmed that `D:\AeroCode1` is an empty non-git directory and can be used as a clean new project root.
2. Verified the actual local tools and versions that matter to AxerCode.
3. Identified a hidden build risk: Maven currently runs under Java 8 by default.
4. Added repository files that document the architecture direction and lock the development baseline.
5. Added helper scripts so later Maven work can be forced onto JDK 21.

## What We Verified

- JDK 21 is available at `C:\Program Files\Java\jdk-21`
- Maven 3.9.12 is available, but defaults to Java 8 without an override
- Node.js is available at `D:\nodejss\node.exe`
- Ollama is installed and `qwen2.5:7b` is available locally
- Git is installed and ready for repository initialization

## Why This Step Matters

If we skip this step, later code generation may silently compile or run against the wrong Java runtime. That would create confusing build failures once Spring Boot 3.x and Java 21 features are introduced.

Locking the baseline now gives the later CLI, provider, storage, and agent steps a stable foundation.

## Files Added in This Step

- `.gitignore`
- `README.md`
- `scripts/mvn-jdk21.cmd`
- `scripts/verify-baseline.cmd`
- `docs/superpowers/specs/2026-04-18-axercode-design.md`
- `docs/superpowers/plans/2026-04-18-axercode-baseline-implementation.md`

## How the Helper Scripts Work

### `scripts/mvn-jdk21.cmd`

This wrapper overrides `JAVA_HOME` and prepends JDK 21 to `PATH` before calling the fixed Maven launcher path. It resolves the Maven location through `%USERPROFILE%` so the script avoids hardcoded non-ASCII user directory issues in `cmd.exe`.

### `scripts/verify-baseline.cmd`

This script validates the minimum local prerequisites for AxerCode:

- JDK 21 exists
- Maven launcher exists
- Maven can run under JDK 21
- Ollama exists on PATH
- `qwen2.5:7b` is installed locally

## Result

The repository now has a frozen baseline and can move safely into the next step: scope freezing and Maven multi-module scaffolding.

## Next Step

Step 2 will turn this baseline into the first concrete project skeleton:

- initialize Git for the new repository
- create the parent Maven project
- define the initial module boundaries
- prepare the first build that runs under JDK 21

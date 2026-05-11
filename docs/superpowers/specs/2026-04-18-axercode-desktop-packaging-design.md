# AxerCode Desktop Packaging Design

## Goal

Produce the first real Windows desktop packaging layer for AxerCode while staying honest about the current machine constraints.

## Constraints

- Rust/Cargo are not installed.
- No visible MSVC / Visual Studio Build Tools are installed.
- JDK 21 is available locally and includes `jpackage`.
- The project already has a working `desktop` Spring profile that binds to localhost and opens the UI.

## Recommended Approach

Use a hybrid strategy for Step 29:

1. create a real Windows desktop package now with `jpackage`
2. add a Pake workspace and build scripts so the Pake route remains ready
3. defer the actual Pake binary build until Rust/Cargo and Windows C++ prerequisites are present

## Why This Approach

This approach preserves forward progress and avoids fake completion:

- `jpackage` can produce a working Windows app image immediately using the existing JDK 21 toolchain
- the packaged runtime still uses the AxerCode `desktop` profile and current web UI
- the Pake path stays explicit and versioned in the repo instead of being dropped

## Deliverables

### Working Artifact

A Windows `app-image` produced by `jpackage`:

- launches the Spring Boot fat jar
- passes `--spring.profiles.active=desktop`
- behaves like the current desktop preview runtime

### Pake Scaffold

A local Node workspace for Pake:

- pinned `pake-cli`
- scriptable build commands
- friendly preflight behavior for missing Rust/Cargo
- D-drive Rust install helper scripts for later use

## Boundaries

Step 29 includes:

- Windows `jpackage` build scripts
- Pake workspace scaffolding
- Rust-on-D install helpers
- packaging docs and verification

Step 29 does not include:

- a successful Pake binary build on this machine unless prerequisites become available
- native-image backend bundling
- installer polishing, icons, or updater behavior

## Follow-On

Step 30 can build on this by:

- producing a GraalVM native backend
- replacing the fat-jar backend in packaged flows
- revisiting the Pake build once Rust/Cargo and linker prerequisites are installed

# AxerCode Desktop Preview Design

## Goal

Introduce a desktop-preview runtime for Windows before real Pake packaging exists. The preview should keep AxerCode local-only, launch the existing Spring Boot server in a desktop profile, and open the minimalist web UI automatically in the default browser.

## Why This Step Exists

Rust/Cargo and Pake are not installed yet, but the project still needs a stable runtime shape for later desktop wrapping. Step 28 builds that shape now so Step 29 can focus on packaging rather than inventing runtime behavior.

## Scope

This step includes:

- a `desktop` Spring profile for the server
- desktop-specific properties and URL building
- startup-time browser launching for the local UI
- Windows launcher scripts for desktop preview
- tests and docs for the preview flow

This step does not include:

- Rust or Cargo installation
- Pake packaging
- an installer, updater, tray icon, or native window chrome

## Architecture

### Desktop Runtime Model

The existing `axercode-server` remains the only runtime. Desktop preview is a specialized startup mode:

1. run `axercode-server` with `spring.profiles.active=desktop`
2. bind the HTTP server to `127.0.0.1`
3. after the embedded server is ready, compute the local UI URL
4. open that URL in the user’s default browser

### Responsibilities

- `DesktopApplicationProperties`: desktop-mode configuration such as launch toggle and UI path
- `DesktopUrlBuilder`: normalize the local UI URL from host, port, and path
- `DesktopStartupLauncher`: open the browser when desktop launch is enabled
- `DesktopApplicationReadyListener`: bridge Spring’s ready event to the launcher
- `application-desktop.yml`: desktop-safe defaults
- launcher scripts: Windows entry point for previewing the desktop runtime

## Behavior

### Desktop Profile

The `desktop` profile should:

- bind to `127.0.0.1`
- use a stable desktop preview port
- leave the existing web UI at `/`
- default browser launch to enabled

### Browser Launching

When the desktop profile starts successfully:

- the app should build the local URL from actual runtime port and configured path
- the browser should open once
- failures to open the browser should be logged but must not crash the server

### Launcher Script

The Windows launcher should:

- build the server jar with the pinned JDK 21 Maven wrapper script
- start the packaged server with the `desktop` profile
- allow extra JVM or Spring args to be forwarded

## Testing

This step should verify:

- URL normalization for desktop launch targets
- browser launch enable/disable behavior
- event-to-launch forwarding on application ready
- static/manual verification that packaged desktop preview starts and serves the UI

## Follow-On Work

Step 29 can wrap this runtime with Pake once Rust/Cargo is available. Because desktop preview already standardizes localhost binding and browser entry behavior, later packaging can stay thin.

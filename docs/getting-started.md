# AxerCode Getting Started

This guide explains what a new user needs to change after cloning the repository, what prerequisites are required, and how to rebuild the four tracked Windows deliverables.

## 1. Clone the Repository

```powershell
git lfs install
git clone https://github.com/Hu-Weiming/AxeroCode1.git
cd AxeroCode1
```

If you do not care about the tracked packaged binaries under `AAAstart/`, Git LFS is optional and you can rebuild those outputs locally instead.

## 2. Minimum Prerequisites

### Source Build and Local Usage

- Windows 10 or Windows 11
- Git LFS if you want the tracked `AAAstart/AxerCode.exe/` desktop runtime to download completely
- JDK 21
- Maven 3.9 or newer
- Ollama installed and running
- A local Ollama model, with `qwen2.5:7b` matching the current default configuration unless you change it

### Native and Desktop Packaging

To rebuild every tracked artifact under `AAAstart/`, you also need:

- GraalVM 21 with `native-image`
- Visual Studio Build Tools with the Windows linker toolchain
- Node.js
- Rust/Cargo
- WiX is helpful for full Windows packaging flows, although the repository script can still capture the Pake executable if the final bundle step fails

## 3. What You May Need To Modify

### If You Stay on Local Ollama

If your machine already exposes Ollama at `http://127.0.0.1:11434` and you have the `qwen2.5:7b` model installed, you do not need to change any source configuration to get started.

If your local model name is different, change:

- `axercode-cli/src/main/resources/application.yml`
- `axercode-server/src/main/resources/application.yml`

Look for:

- `axercode.provider.default-model`
- `axercode.provider.ollama.default-model`

If your Ollama base URL is different, change:

- `axercode.provider.ollama.base-url`

### If You Want Anthropic

Server-side web and desktop flow:

```powershell
$env:ANTHROPIC_API_KEY = 'your-real-key'
```

CLI-side flow:

```powershell
$env:AXERCODE_PROVIDER_ANTHROPIC_API_KEY = 'your-real-key'
```

You can also change defaults in:

- `axercode-server/src/main/resources/application.yml`
- `axercode-cli/src/main/resources/application.yml`

### If You Want OpenAI-Compatible Usage

Important: the repository currently contains an OpenAI-compatible placeholder provider, not a finished provider implementation.

That means the UI routing exists, but the actual provider behavior is still incomplete.

If you still want to wire configuration for future work, the corresponding values are:

- Server env var: `OPENAI_API_KEY`
- CLI env var: `AXERCODE_PROVIDER_OPENAI_API_KEY`

### If You Want a Different Storage Location

Default persistence paths:

- CLI SQLite file: `%USERPROFILE%\.axercode\data\axercode.db`
- CLI history file: `%USERPROFILE%\.axercode\cli.history`
- Server SQLite file: `%USERPROFILE%\.axercode\data\axercode.db`

You can override them in:

- `axercode-cli/src/main/resources/application.yml`
- `axercode-server/src/main/resources/application.yml`

### If Port 19090 Is Occupied

The desktop preview profile defaults to `127.0.0.1:19090`.

Change:

- `axercode-server/src/main/resources/application-desktop.yml`

## 4. Build and Test from Source

Run the full Maven test suite:

```powershell
.\scripts\mvn-jdk21.cmd -q test
```

## 5. Launch the Main Experiences

### CLI

Build:

```powershell
.\scripts\mvn-jdk21.cmd -q -pl axercode-cli -am package
```

Run one-shot:

```powershell
& 'C:\Program Files\Java\jdk-21\bin\java.exe' -jar .\axercode-cli\target\axercode-cli-0.1.0-SNAPSHOT.jar --prompt "Explain the project structure."
```

Run interactive:

```powershell
& 'C:\Program Files\Java\jdk-21\bin\java.exe' -jar .\axercode-cli\target\axercode-cli-0.1.0-SNAPSHOT.jar --interactive
```

### Web/Desktop Preview

```powershell
.\scripts\launch-desktop-preview.ps1
```

Then open:

- `http://127.0.0.1:19090/`

## 6. Rebuild the Four Tracked Windows Artifacts

The repository keeps the final Windows binaries in `AAAstart/`.

### Native CLI

Build:

```powershell
.\scripts\build-native-cli.ps1
```

Expected output:

- `axercode-cli\target\axercode-cli.exe`
- `axercode-cli\target\sqlitejdbc.dll`

Tracked release location:

- `AAAstart/axercode-cli.exe/`

### Native Server

Build:

```powershell
.\scripts\build-native-server.ps1
```

Expected output:

- `axercode-server\target\axercode-server.exe`
- `axercode-server\target\sqlitejdbc.dll`

Tracked release location:

- `AAAstart/axercode-server.exe/`

### jpackage Desktop App

Build:

```powershell
.\scripts\build-desktop-app-image.ps1
```

Expected output:

- `dist\desktop\AxerCode\`

Tracked release location:

- `AAAstart/AxerCode.exe/`

### Pake Desktop Shell

Build:

```powershell
.\scripts\build-pake-shell.ps1
```

Expected output:

- `dist\desktop\pake\AxerCode-Pake.exe`

Tracked release location:

- `AAAstart/AxerCode-Pake.exe/`

## 7. Directly Run the Tracked Artifacts

If you do not want to build from source, you can use the tracked Windows outputs directly:

- `AAAstart/axercode-cli.exe/axercode-cli.exe`
- `AAAstart/axercode-server.exe/axercode-server.exe`
- `AAAstart/AxerCode.exe/AxerCode.exe`
- `AAAstart/AxerCode-Pake.exe/AxerCode-Pake.exe`

## 8. Before You Publish Your Own Fork

Do a quick safety pass:

- Keep real API keys out of tracked files.
- Keep local databases out of the repository.
- Keep `node_modules`, `target`, and `dist` build caches out of version control.
- Choose and add an open-source license if you want third parties to reuse the project legally.

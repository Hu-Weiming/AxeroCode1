# AxerCode Pake Shell

This workspace packages the AxerCode web UI into a Windows desktop shell using Pake.

## What This Folder Is For

- The main source of truth for AxerCode still lives in the Java modules under the repository root.
- This sub-workspace exists only for the Pake desktop wrapper build.
- The tracked final executable is copied into `AAAstart/AxerCode-Pake.exe/`.

## Prerequisites

To rebuild the Pake shell on Windows, make sure you have:

- Node.js
- Rust/Cargo
- Visual Studio Build Tools with the Windows C/C++ toolchain

WiX is also useful for full installer-oriented packaging flows.

## Install Dependencies

```powershell
cd desktop\pake-shell
D:\nodejss\npm.cmd install
```

## Build

From the repository root:

```powershell
.\scripts\build-pake-shell.ps1
```

Expected output:

- `dist\desktop\pake\AxerCode-Pake.exe`

Tracked release location:

- `AAAstart/AxerCode-Pake.exe\AxerCode-Pake.exe`

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = (Resolve-Path (Join-Path $scriptDir "..")).Path
$workspace = Join-Path $repoRoot "desktop\\pake-shell"
$distDir = Join-Path $repoRoot "dist\\desktop\\pake"
$builtExe = Join-Path $workspace "node_modules\\pake-cli\\src-tauri\\target\\x86_64-pc-windows-msvc\\release\\pake-axercode.exe"
$wixDir = 'D:\\WiX\\wix314'
$cargoHome = [Environment]::GetEnvironmentVariable("CARGO_HOME", "User")
$rustupHome = [Environment]::GetEnvironmentVariable("RUSTUP_HOME", "User")
if ($cargoHome) {
    $env:CARGO_HOME = $cargoHome
    $env:Path = (Join-Path $cargoHome "bin") + ";" + $env:Path
}
if ($rustupHome) {
    $env:RUSTUP_HOME = $rustupHome
}
if ((Test-Path (Join-Path $wixDir 'candle.exe')) -and (Test-Path (Join-Path $wixDir 'light.exe'))) {
    $env:Path = $wixDir + ";" + $env:Path
}
$vcvars = 'D:\\BuildTools2022\\VC\\Auxiliary\\Build\\vcvars64.bat'

if (-not (Get-Command cargo -ErrorAction SilentlyContinue)) {
    Write-Error "cargo not found. Install Rust to D: first with scripts\\install-rust-on-d.ps1, and ensure Windows C++ build tools are available."
    exit 1
}

if (-not (Test-Path (Join-Path $workspace "node_modules\\pake-cli"))) {
    & "D:\\nodejss\\npm.cmd" install
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

Push-Location $workspace
try {
    New-Item -ItemType Directory -Force -Path $distDir | Out-Null
    if ((-not (Get-Command cl -ErrorAction SilentlyContinue)) -and (Test-Path $vcvars)) {
        cmd /c "call `"$vcvars`" >nul && call `"D:\\nodejss\\npm.cmd`" run build:shell"
        $buildExitCode = $LASTEXITCODE
    } else {
        & "D:\\nodejss\\npm.cmd" run build:shell
        $buildExitCode = $LASTEXITCODE
    }

    if (Test-Path $builtExe) {
        Copy-Item -LiteralPath $builtExe -Destination (Join-Path $distDir "AxerCode-Pake.exe") -Force
        if ($buildExitCode -ne 0) {
            Write-Warning "Pake produced AxerCode-Pake.exe, but the final bundle step failed. Using the built executable artifact from dist\\desktop\\pake."
            exit 0
        }
    }

    exit $buildExitCode
}
finally {
    Pop-Location
}

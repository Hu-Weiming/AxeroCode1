$rustRoot = "D:\\Rust"
$cargoHome = Join-Path $rustRoot ".cargo"
$rustupHome = Join-Path $rustRoot ".rustup"
$bootstrapDir = Join-Path $rustRoot "bootstrap"
$installer = Join-Path $bootstrapDir "rustup-init.exe"

New-Item -ItemType Directory -Force -Path $cargoHome | Out-Null
New-Item -ItemType Directory -Force -Path $rustupHome | Out-Null
New-Item -ItemType Directory -Force -Path $bootstrapDir | Out-Null

[Environment]::SetEnvironmentVariable("CARGO_HOME", $cargoHome, "User")
[Environment]::SetEnvironmentVariable("RUSTUP_HOME", $rustupHome, "User")
$env:CARGO_HOME = $cargoHome
$env:RUSTUP_HOME = $rustupHome

Invoke-WebRequest -Uri "https://win.rustup.rs/x86_64" -OutFile $installer
& $installer -y --profile minimal
if ($LASTEXITCODE -eq 0) {
    & (Join-Path $cargoHome "bin\\rustup.exe") default stable | Out-Null
}
exit $LASTEXITCODE

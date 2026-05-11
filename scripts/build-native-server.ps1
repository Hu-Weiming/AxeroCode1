$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = (Resolve-Path (Join-Path $scriptDir "..")).Path
$mvnCmd = Join-Path $repoRoot "scripts\\mvn-jdk21.cmd"
$nativeTemp = Join-Path $repoRoot "target\\native-temp"
$nativeUserHome = Join-Path $repoRoot "target\\native-user-home"
$graalHome = $env:GRAALVM_HOME
if (-not $graalHome) {
    $graalHome = [Environment]::GetEnvironmentVariable("GRAALVM_HOME", "User")
    if ($graalHome) {
        $env:GRAALVM_HOME = $graalHome
    }
}
$vcvars = 'D:\\BuildTools2022\\VC\\Auxiliary\\Build\\vcvars64.bat'

$nativeImage = Get-Command native-image -ErrorAction SilentlyContinue
if (-not $nativeImage -and $graalHome) {
    $candidate = Join-Path $graalHome "bin\\native-image.cmd"
    if (Test-Path $candidate) {
        $nativeImage = Get-Item $candidate
        $env:Path = (Join-Path $graalHome "bin") + ";" + $env:Path
    }
}

if (-not $nativeImage) {
    Write-Error "native-image not found. Install GraalVM to D: with scripts\\install-graalvm-on-d.ps1 first."
    exit 1
}

New-Item -ItemType Directory -Force -Path $nativeTemp, $nativeUserHome | Out-Null
$env:TEMP = $nativeTemp
$env:TMP = $nativeTemp
$env:NATIVE_IMAGE_USER_HOME = $nativeUserHome

if ((-not (Get-Command cl -ErrorAction SilentlyContinue)) -and (-not (Test-Path $vcvars))) {
    Write-Error "Microsoft C/C++ build tools were not found. Native server build requires the Windows linker toolchain."
    exit 1
}

if (-not (Get-Command cl -ErrorAction SilentlyContinue)) {
    cmd /c "set ""TEMP=$nativeTemp"" && set ""TMP=$nativeTemp"" && set ""NATIVE_IMAGE_USER_HOME=$nativeUserHome"" && call `"$vcvars`" >nul && call `"$mvnCmd`" -q -pl axercode-server -am -Pnative package"
    exit $LASTEXITCODE
}

& $mvnCmd -q -pl axercode-server -am -Pnative package
exit $LASTEXITCODE

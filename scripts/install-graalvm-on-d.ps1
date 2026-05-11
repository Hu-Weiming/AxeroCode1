param(
    [string]$ZipPath
)

if (-not $ZipPath) {
    Write-Error "Download an official GraalVM for JDK 21 Windows ZIP, then rerun with -ZipPath <path-to-zip>. Install target will be D:\\GraalVM."
    exit 1
}

$resolvedZip = (Resolve-Path $ZipPath).Path
$installRoot = "D:\\GraalVM"

New-Item -ItemType Directory -Force -Path $installRoot | Out-Null
Expand-Archive -Path $resolvedZip -DestinationPath $installRoot -Force

$graalHome = Get-ChildItem $installRoot -Directory | Sort-Object LastWriteTime -Descending | Select-Object -First 1 -ExpandProperty FullName
[Environment]::SetEnvironmentVariable("GRAALVM_HOME", $graalHome, "User")
$env:GRAALVM_HOME = $graalHome

Write-Output "GRAALVM_HOME set to $graalHome"

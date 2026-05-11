$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = (Resolve-Path (Join-Path $scriptDir "..")).Path
$mvnCmd = Join-Path $repoRoot "scripts\\mvn-jdk21.cmd"
$jpackageExe = "C:\\Program Files\\Java\\jdk-21\\bin\\jpackage.exe"
$outputDir = Join-Path $repoRoot "dist\\desktop"
$stagingDir = Join-Path $repoRoot "dist\\desktop-staging"
$jarPath = Join-Path $repoRoot "axercode-server\\target\\axercode-server-0.1.0-SNAPSHOT.jar"
$mainJar = "axercode-server-0.1.0-SNAPSHOT.jar"
$appDir = Join-Path $outputDir "AxerCode"

& $mvnCmd -q -pl axercode-server -am package
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
New-Item -ItemType Directory -Force -Path $stagingDir | Out-Null
if (Test-Path $appDir) {
    Remove-Item -Recurse -Force $appDir
}
Get-ChildItem -Force $stagingDir | Remove-Item -Recurse -Force
Copy-Item -Path $jarPath -Destination (Join-Path $stagingDir $mainJar) -Force

& $jpackageExe `
  --type app-image `
  --name AxerCode `
  --dest $outputDir `
  --input $stagingDir `
  --main-jar $mainJar `
  --app-version 0.1.0 `
  --vendor AxerCode `
  --description "AxerCode local desktop packaging bridge." `
  --arguments "--spring.profiles.active=desktop" `
  --arguments "--server.port=19090" `
  --win-console

exit $LASTEXITCODE

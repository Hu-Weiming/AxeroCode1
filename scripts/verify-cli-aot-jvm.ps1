$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = (Resolve-Path (Join-Path $scriptDir "..")).Path
$mvnCmd = Join-Path $repoRoot "scripts\\mvn-jdk21.cmd"
$javaExe = "C:\\Program Files\\Java\\jdk-21\\bin\\java.exe"
$jarPath = Join-Path $repoRoot "axercode-cli\\target\\axercode-cli-0.1.0-SNAPSHOT.jar"

& $mvnCmd -q -pl axercode-cli -am -Paot package
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$output = & $javaExe "-Dspring.aot.enabled=true" -jar $jarPath --help 2>&1
$text = ($output | Out-String)
if ($LASTEXITCODE -ne 0 -or $text -notmatch 'Usage:' -or $text -notmatch 'AxerCode') {
    Write-Error $text
    exit 1
}

Write-Output "AOT_CLI_OK"

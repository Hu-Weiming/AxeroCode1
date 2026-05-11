$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = (Resolve-Path (Join-Path $scriptDir "..")).Path
$mvnCmd = Join-Path $repoRoot "scripts\\mvn-jdk21.cmd"
$serverJar = Join-Path $repoRoot "axercode-server\\target\\axercode-server-0.1.0-SNAPSHOT.jar"
$javaExe = "C:\\Program Files\\Java\\jdk-21\\bin\\java.exe"

& $mvnCmd -q -pl axercode-server -am package
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

& $javaExe -jar $serverJar "--spring.profiles.active=desktop" @args
exit $LASTEXITCODE

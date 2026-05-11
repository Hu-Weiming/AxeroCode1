$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = (Resolve-Path (Join-Path $scriptDir "..")).Path
$mvnCmd = Join-Path $repoRoot "scripts\\mvn-jdk21.cmd"
$javaExe = "C:\\Program Files\\Java\\jdk-21\\bin\\java.exe"
$jarPath = Join-Path $repoRoot "axercode-server\\target\\axercode-server-0.1.0-SNAPSHOT.jar"
$outputDir = Join-Path $repoRoot "target\\step30"
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
$stdout = Join-Path $outputDir "server-aot.out.log"
$stderr = Join-Path $outputDir "server-aot.err.log"

& $mvnCmd -q -pl axercode-server -am -Paot package
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$proc = Start-Process -FilePath $javaExe -ArgumentList @(
    '-Dspring.aot.enabled=true',
    '-jar', $jarPath,
    '--spring.profiles.active=desktop',
    '--axercode.desktop.launch-on-startup=false',
    '--server.port=19093'
) -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru

try {
    $ok = $false
    for ($i = 0; $i -lt 40; $i++) {
        Start-Sleep -Seconds 1
        try {
            $response = Invoke-WebRequest -Uri 'http://127.0.0.1:19093/api/health' -UseBasicParsing -TimeoutSec 2
            if ($response.StatusCode -eq 200 -and $response.Content -match '"status":"ok"') {
                $ok = $true
                break
            }
        } catch {
        }
    }

    if (-not $ok) {
        if (Test-Path $stdout) { Get-Content $stdout }
        if (Test-Path $stderr) { Get-Content $stderr }
        Write-Error "AOT server did not become healthy in time."
        exit 1
    }

    Write-Output "AOT_SERVER_OK"
} finally {
    if ($proc -and -not $proc.HasExited) {
        Stop-Process -Id $proc.Id -Force
    }
}

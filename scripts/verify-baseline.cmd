@echo off
setlocal

set "JAVA_HOME=C:\Program Files\Java\jdk-21"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "MAVEN_CMD=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.12-bin\5nmfsn99br87k5d4ajlekdq10k\apache-maven-3.9.12\bin\mvn.cmd"
set "EXPECTED_MODEL=qwen2.5:7b"

echo [AxerCode] Verifying local baseline...

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo [AxerCode] Missing JDK 21 at "%JAVA_HOME%".
  exit /b 1
)

if not exist "%MAVEN_CMD%" (
  echo [AxerCode] Missing Maven launcher at "%MAVEN_CMD%".
  exit /b 1
)

java -version
if errorlevel 1 exit /b 1

call "%MAVEN_CMD%" -version
if errorlevel 1 exit /b 1

where.exe ollama
if errorlevel 1 (
  echo [AxerCode] Ollama executable was not found on PATH.
  exit /b 1
)

ollama list | findstr /C:"%EXPECTED_MODEL%" >nul
if errorlevel 1 (
  echo [AxerCode] Expected Ollama model "%EXPECTED_MODEL%" is not installed.
  exit /b 1
)

echo [AxerCode] Baseline verification passed.

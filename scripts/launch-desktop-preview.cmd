@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "REPO_ROOT=%%~fI"
set "MVN_CMD=%REPO_ROOT%\scripts\mvn-jdk21.cmd"
set "SERVER_JAR=%REPO_ROOT%\axercode-server\target\axercode-server-0.1.0-SNAPSHOT.jar"
set "JAVA_EXE=C:\Program Files\Java\jdk-21\bin\java.exe"

call "%MVN_CMD%" -q -pl axercode-server -am package
if errorlevel 1 exit /b %errorlevel%

"%JAVA_EXE%" -jar "%SERVER_JAR%" --spring.profiles.active=desktop %*
exit /b %errorlevel%

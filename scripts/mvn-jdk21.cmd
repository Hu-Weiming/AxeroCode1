@echo off
setlocal

set "JAVA_HOME=C:\Program Files\Java\jdk-21"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "MAVEN_CMD=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.12-bin\5nmfsn99br87k5d4ajlekdq10k\apache-maven-3.9.12\bin\mvn.cmd"

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo [AxerCode] JDK 21 not found at "%JAVA_HOME%".
  exit /b 1
)

if not exist "%MAVEN_CMD%" (
  echo [AxerCode] Maven launcher not found at "%MAVEN_CMD%".
  exit /b 1
)

call "%MAVEN_CMD%" %*
exit /b %ERRORLEVEL%

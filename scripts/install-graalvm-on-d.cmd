@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0install-graalvm-on-d.ps1" %*
exit /b %errorlevel%

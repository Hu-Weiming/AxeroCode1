@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0verify-cli-aot-jvm.ps1" %*
exit /b %errorlevel%

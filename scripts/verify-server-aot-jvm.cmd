@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0verify-server-aot-jvm.ps1" %*
exit /b %errorlevel%

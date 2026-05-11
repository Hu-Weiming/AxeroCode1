@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0build-pake-shell.ps1" %*
exit /b %errorlevel%

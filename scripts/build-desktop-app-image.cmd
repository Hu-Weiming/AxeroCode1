@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0build-desktop-app-image.ps1" %*
exit /b %errorlevel%

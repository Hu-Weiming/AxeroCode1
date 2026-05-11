@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0build-native-cli.ps1" %*
exit /b %errorlevel%

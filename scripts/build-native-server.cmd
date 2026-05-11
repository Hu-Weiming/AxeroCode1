@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0build-native-server.ps1" %*
exit /b %errorlevel%

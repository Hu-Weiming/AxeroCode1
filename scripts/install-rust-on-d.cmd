@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0install-rust-on-d.ps1" %*
exit /b %errorlevel%

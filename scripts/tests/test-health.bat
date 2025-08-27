@echo off
REM Test Health Endpoint
REM This batch file runs the PowerShell script to test the health endpoint

powershell.exe -ExecutionPolicy Bypass -File "%~dp0test-health.ps1" %*

echo.
echo Press any key to exit...
pause >nul

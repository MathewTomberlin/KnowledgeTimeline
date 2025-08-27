@echo off
REM Test Models Endpoint
REM This batch file runs the PowerShell script to test the models endpoint

powershell.exe -ExecutionPolicy Bypass -File "%~dp0test-models.ps1" %*

echo.
echo Press any key to exit...
pause >nul

@echo off
REM Test Knowledge Object Creation
REM This batch file runs the PowerShell script to test knowledge object creation

powershell.exe -ExecutionPolicy Bypass -File "%~dp0test-knowledge-create.ps1" %*

echo.
echo Press any key to exit...
pause >nul

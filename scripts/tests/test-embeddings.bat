@echo off
REM Test Embeddings Endpoint
REM This batch file runs the PowerShell script to test the embeddings endpoint

powershell.exe -ExecutionPolicy Bypass -File "%~dp0test-embeddings.ps1" %*

echo.
echo Press any key to exit...
pause >nul

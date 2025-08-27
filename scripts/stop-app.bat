@echo off
echo Stopping Knowledge-Aware LLM Middleware Application...
echo.

powershell -ExecutionPolicy Bypass -File "%~dp0stop-app.ps1"

echo.
echo Press any key to exit...
pause >nul

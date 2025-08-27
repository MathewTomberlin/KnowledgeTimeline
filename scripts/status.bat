@echo off
echo Knowledge-Aware LLM Middleware - Service Status
echo ==================================================
echo.

powershell -ExecutionPolicy Bypass -File "%~dp0status.ps1"

echo.
echo Press any key to exit...
pause >nul

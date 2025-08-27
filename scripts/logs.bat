@echo off
echo Knowledge-Aware LLM Middleware - Service Logs
echo ===============================================
echo.

powershell -ExecutionPolicy Bypass -File "%~dp0logs.ps1" %*

echo.
echo Press any key to exit...
pause >nul

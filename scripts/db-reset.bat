@echo off
echo Knowledge-Aware LLM Middleware - Database Reset
echo ==================================================
echo.

powershell -ExecutionPolicy Bypass -File "%~dp0db-reset.ps1" %*

echo.
echo Press any key to exit...
pause >nul

@echo off
REM Run All Tests
REM This batch file runs the comprehensive test suite

powershell.exe -ExecutionPolicy Bypass -File "%~dp0run-all-tests.ps1" %*

echo.
echo Press any key to exit...
pause >nul

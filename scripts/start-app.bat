@echo off
echo Starting Knowledge-Aware LLM Middleware Application...
echo.
cd /d "%~dp0\.."

echo Checking if PowerShell is available...
powershell -Command "Write-Host 'PowerShell is working'" 2>nul
if %ERRORLEVEL% neq 0 (
    echo ERROR: PowerShell is not available or not working properly
    echo Please ensure PowerShell is installed and accessible
    pause
    exit /b 1
)

echo PowerShell is working, executing start script...
echo.
powershell -ExecutionPolicy Bypass -File "scripts\start-app.ps1"
set EXIT_CODE=%ERRORLEVEL%

echo.
if %EXIT_CODE% equ 0 (
    echo Script completed successfully with exit code: %EXIT_CODE%
) else (
    echo Script completed with exit code: %EXIT_CODE%
)

echo.
echo Batch file completed.
pause

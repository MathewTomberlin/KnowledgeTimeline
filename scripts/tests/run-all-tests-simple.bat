@echo off
REM Run All Tests Simple - Batch Wrapper
REM Calls the PowerShell script with the same parameters

powershell.exe -ExecutionPolicy Bypass -File "%~dp0run-all-tests-simple.ps1" %*

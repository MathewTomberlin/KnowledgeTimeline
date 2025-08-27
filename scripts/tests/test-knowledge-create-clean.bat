@echo off
REM Test Knowledge Create Clean - Batch Wrapper
REM Calls the PowerShell script with the same parameters

powershell.exe -ExecutionPolicy Bypass -File "%~dp0test-knowledge-create-clean.ps1" %*

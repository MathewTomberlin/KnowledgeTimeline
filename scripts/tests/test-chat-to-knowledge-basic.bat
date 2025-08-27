@echo off
REM Test Chat to Knowledge Object Creation (Basic) - Batch Wrapper
REM Calls the PowerShell script with the same parameters

powershell.exe -ExecutionPolicy Bypass -File "%~dp0test-chat-to-knowledge-basic.ps1" %*

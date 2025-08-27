@echo off
REM Test Embeddings Clean - Batch Wrapper
REM Calls the PowerShell script with the same parameters

powershell.exe -ExecutionPolicy Bypass -File "%~dp0test-embeddings-clean.ps1" %*

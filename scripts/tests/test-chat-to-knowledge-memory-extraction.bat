@echo off
REM Test Memory Extraction from Chat Conversations - Batch Wrapper
REM Calls the PowerShell script with the same parameters

powershell.exe -ExecutionPolicy Bypass -File "%~dp0test-chat-to-knowledge-memory-extraction.ps1" %*

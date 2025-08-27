#!/usr/bin/env pwsh

# Import common functions
try {
    . "$PSScriptRoot\common.ps1"
} catch {
    Write-Host "Failed to import common functions: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Main execution
Write-Host "Knowledge-Aware LLM Middleware - Service Status" -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Green

# Check if Docker is running
if (-not (Test-DockerRunning)) {
    Write-Host "Docker Desktop is not running" -ForegroundColor Red
    exit 1
}

# Show status of all services
$servicesToCheck = @("postgres", "redis", "ollama", "middleware")
Get-ServiceStatus $servicesToCheck

Write-Host "`nStatus check completed!" -ForegroundColor Green

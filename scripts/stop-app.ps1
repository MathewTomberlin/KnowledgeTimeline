#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Stop the Knowledge-Aware LLM Middleware application and all services.

.DESCRIPTION
    This script stops all running services for the Knowledge-Aware LLM Middleware:
    - Main middleware application
    - Embeddings service
    - Redis
    - PostgreSQL
    
    The script handles graceful shutdown and cleanup.

.PARAMETER Services
    Comma-separated list of specific services to stop. Default: all services.
    Valid values: postgres, redis, embeddings, middleware

.PARAMETER Force
    Force stop containers without graceful shutdown.

.PARAMETER RemoveVolumes
    Remove data volumes when stopping (WARNING: This will delete all data).

.EXAMPLE
    .\stop-app.ps1
    
    Stop all services gracefully.

.EXAMPLE
    .\stop-app.ps1 -Services middleware,embeddings
    
    Stop only the middleware and embeddings services.

.EXAMPLE
    .\stop-app.ps1 -Force -RemoveVolumes
    
    Force stop all services and remove all data (DANGEROUS!).
#>

param(
    [Parameter()]
    [string]$Services = "middleware,ollama,redis,postgres",
    
    [Parameter()]
    [switch]$Force,
    
    [Parameter()]
    [switch]$RemoveVolumes
)



# Import common functions
try {
    . "$PSScriptRoot\common.ps1"
} catch {
    Write-Host "Failed to import common functions: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   Make sure you're running this script from the project root directory" -ForegroundColor Yellow
    exit 1
}

# Wrap the entire script in a try-catch for error handling
try {
    Write-Host "Stopping Knowledge-Aware LLM Middleware Application" -ForegroundColor Red
    Write-Host "=========================================================" -ForegroundColor Red

# Check if Docker is running
if (-not (Test-DockerRunning)) {
    Write-Host "Docker Desktop is not running. Nothing to stop." -ForegroundColor Red
    exit 1
}

# Parse services to stop
$servicesToStop = $Services -split "," | ForEach-Object { $_.Trim() }

# Display current status
Write-Host "`nCurrent Service Status:" -ForegroundColor Yellow
Get-ServiceStatus $servicesToStop

# Confirm removal of volumes if requested
if ($RemoveVolumes) {
    Write-Host "`nWARNING: You are about to remove ALL data volumes!" -ForegroundColor Red
    Write-Host "   This will permanently delete:" -ForegroundColor Red
    Write-Host "   - All database data" -ForegroundColor Red
    Write-Host "   - All Redis cache data" -ForegroundColor Red
    Write-Host "   - All application data" -ForegroundColor Red
    
    $confirmation = Read-Host "`nAre you sure you want to continue? (Type 'YES' to confirm)"
    if ($confirmation -ne "YES") {
        Write-Host "Operation cancelled by user" -ForegroundColor Yellow
        exit 0
    }
    
    Write-Host "Confirmed. Proceeding with data removal..." -ForegroundColor Red
}

# Stop services in reverse dependency order
Write-Host "`nStopping services..." -ForegroundColor Yellow

# 1. Stop Middleware first (depends on others)
if ($servicesToStop -contains "middleware") {
    Write-Host "   Stopping Middleware application..." -ForegroundColor Cyan
    Stop-Service middleware
}

# 2. Stop Ollama
if ($servicesToStop -contains "ollama") {
    Write-Host "   Stopping Ollama service..." -ForegroundColor Cyan
    Stop-Service ollama
}

# 3. Stop Redis
if ($servicesToStop -contains "redis") {
    Write-Host "   Stopping Redis..." -ForegroundColor Cyan
    Stop-Service redis
}

# 4. Stop PostgreSQL last (others depend on it)
if ($servicesToStop -contains "postgres") {
    Write-Host "   Stopping PostgreSQL..." -ForegroundColor Cyan
    Stop-Service postgres
}

# Remove containers and volumes if requested
if ($RemoveVolumes) {
    Write-Host "`nRemoving containers and volumes..." -ForegroundColor Red
    
    try {
        $result = docker-compose down -v 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "All containers and volumes removed" -ForegroundColor Green
        } else {
            Write-Host "Some containers or volumes may not have been removed" -ForegroundColor Yellow
            Write-Host "   Error: $result" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "Error removing containers and volumes: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    # Just stop containers
    try {
        $result = docker-compose stop $servicesToStop 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "All requested services stopped" -ForegroundColor Green
        } else {
            Write-Host "Some services may not have stopped properly" -ForegroundColor Yellow
            Write-Host "   Error: $result" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "Error stopping services: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Display final status
Write-Host "`nFinal Service Status:" -ForegroundColor Cyan
Get-ServiceStatus $servicesToStop

Write-Host "`nApplication stopped successfully!" -ForegroundColor Green

if ($RemoveVolumes) {
    Write-Host "All data has been permanently deleted" -ForegroundColor Red
    Write-Host "   You will need to recreate the database on next startup" -ForegroundColor Yellow
} else {
    Write-Host "Data has been preserved" -ForegroundColor Green
    Write-Host "   Use .\scripts\start-app.ps1 to restart the application" -ForegroundColor White
}

Write-Host "`nUseful commands:" -ForegroundColor Cyan
Write-Host "   Start application: .\scripts\start-app.ps1" -ForegroundColor White
Write-Host "   Check status: .\scripts\status.ps1" -ForegroundColor White
Write-Host "   View logs: .\scripts\logs.ps1" -ForegroundColor White


} catch {
    Write-Host "`nAn unexpected error occurred: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   Stack trace: $($_.ScriptStackTrace)" -ForegroundColor Red
}

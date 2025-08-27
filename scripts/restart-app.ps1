#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Restart the Knowledge-Aware LLM Middleware application and all services.

.DESCRIPTION
    This script restarts all services for the Knowledge-Aware LLM Middleware:
    - Stops all running services
    - Starts all services with proper dependency management
    - Ensures services are healthy after restart

.PARAMETER Services
    Comma-separated list of specific services to restart. Default: all services.
    Valid values: postgres, redis, embeddings, middleware

.PARAMETER Profile
    Spring profile to use for the middleware service. Default: docker.
    Valid values: docker, local, test

.PARAMETER SkipEmbeddings
    Skip starting the embeddings service (useful if it fails on your system).

.PARAMETER Build
    Build the application before restarting.

.EXAMPLE
    .\restart-app.ps1
    
    Restart all services with default settings.

.EXAMPLE
    .\restart-app.ps1 -Services middleware,embeddings
    
    Restart only the middleware and embeddings services.

.EXAMPLE
    .\restart-app.ps1 -Profile local -Build
    
    Restart with local profile and build first.
#>

param(
    [Parameter()]
    [string]$Services = "postgres,redis,embeddings,middleware",
    
    [Parameter()]
    [ValidateSet("docker", "local", "test")]
    [string]$Profile = "docker",
    
    [Parameter()]
    [switch]$SkipEmbeddings,
    
    [Parameter()]
    [switch]$Build
)

# Import common functions
. "$PSScriptRoot\common.ps1"

Write-Host "🔄 Restarting Knowledge-Aware LLM Middleware Application" -ForegroundColor Yellow
Write-Host "=========================================================" -ForegroundColor Yellow

# Check if Docker is running
if (-not (Test-DockerRunning)) {
    Write-Host "❌ Docker Desktop is not running. Please start Docker Desktop first." -ForegroundColor Red
    exit 1
}

# Parse services to restart
$servicesToRestart = $Services -split "," | ForEach-Object { $_.Trim() }

Write-Host "`n📋 Services to restart: $($servicesToRestart -join ', ')" -ForegroundColor Cyan

# Step 1: Stop services
Write-Host "`n🛑 Step 1: Stopping services..." -ForegroundColor Red

# Stop services in reverse dependency order
if ($servicesToRestart -contains "middleware") {
    Write-Host "   Stopping Middleware application..." -ForegroundColor Cyan
    & "$PSScriptRoot\stop-app.ps1" -Services middleware
}

if ($servicesToRestart -contains "embeddings") {
    Write-Host "   Stopping Embeddings service..." -ForegroundColor Cyan
    & "$PSScriptRoot\stop-app.ps1" -Services embeddings
}

if ($servicesToRestart -contains "redis") {
    Write-Host "   Stopping Redis..." -ForegroundColor Cyan
    & "$PSScriptRoot\stop-app.ps1" -Services redis
}

if ($servicesToRestart -contains "postgres") {
    Write-Host "   Stopping PostgreSQL..." -ForegroundColor Cyan
    & "$PSScriptRoot\stop-app.ps1" -Services postgres
}

# Wait a moment for services to fully stop
Write-Host "   Waiting for services to stop..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

# Step 2: Start services
Write-Host "`n🚀 Step 2: Starting services..." -ForegroundColor Green

# Build parameters for start script
$startParams = @{
    Services = $Services
    Profile = $Profile
    WaitForHealthy = $true
}

if ($SkipEmbeddings) {
    $startParams.SkipEmbeddings = $true
}

if ($Build) {
    $startParams.Build = $true
}

# Start services using the start script
& "$PSScriptRoot\start-app.ps1" @startParams

# Check final status
Write-Host "`n📊 Final Status After Restart:" -ForegroundColor Cyan
Get-ServiceStatus $servicesToRestart

Write-Host "`n🔄 Application restart completed!" -ForegroundColor Green
Write-Host "📊 API available at: http://localhost:8080" -ForegroundColor White
Write-Host "📚 Health check: http://localhost:8080/actuator/health" -ForegroundColor White
Write-Host "🔍 Check status: .\scripts\status.ps1" -ForegroundColor White

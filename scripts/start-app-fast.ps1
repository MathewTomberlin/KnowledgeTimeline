#!/usr/bin/env pwsh

param(
    [string]$Services = "postgres,redis,middleware",
    [switch]$SkipOllama,
    [switch]$WaitForHealthy = $true
)

Write-Host "Knowledge-Aware LLM Middleware - Fast Startup" -ForegroundColor Green
Write-Host "==============================================" -ForegroundColor Green
Write-Host "This script starts services quickly by skipping Ollama model loading" -ForegroundColor Yellow
Write-Host "Use this for development when you want faster startup times" -ForegroundColor Yellow
Write-Host "" -ForegroundColor Yellow

try {
    # Load common functions
    if (Test-Path "$PSScriptRoot\common.ps1") {
        . "$PSScriptRoot\common.ps1"
    } else {
        Write-Host "Common functions file not found" -ForegroundColor Red
        exit 1
    }

    Write-Host "Checking prerequisites..." -ForegroundColor Yellow

    if (-not (Test-DockerRunning)) {
        Write-Host "Docker Desktop is not running" -ForegroundColor Red
        exit 1
    }

    if (-not (Test-PortsAvailable)) {
        Write-Host "Required ports are not available" -ForegroundColor Red
        exit 1
    }

    Write-Host "Prerequisites check passed" -ForegroundColor Green

    $servicesToStart = $Services -split "," | ForEach-Object { $_.Trim() }

    # Add Ollama to services only if not skipped
    if (-not $SkipOllama) {
        if (-not ($servicesToStart -contains "ollama")) {
            $servicesToStart += "ollama"
            Write-Host "Including Ollama service" -ForegroundColor Yellow
        }
    } else {
        Write-Host "Skipping Ollama service (faster startup)" -ForegroundColor Yellow
        $servicesToStart = $servicesToStart | Where-Object { $_ -ne "ollama" }
    }

    # Create a list of services to check for health (exclude ollama for fast mode)
    if ($SkipOllama) {
        $servicesToCheck = $servicesToStart | Where-Object { $_ -ne "ollama" }
    } else {
        $servicesToCheck = $servicesToStart
    }

    Write-Host "Starting services (fast mode)..." -ForegroundColor Yellow

    # Start PostgreSQL first
    if ($servicesToStart -contains "postgres") {
        Write-Host "Starting PostgreSQL..." -ForegroundColor Cyan
        if (-not (Start-Service postgres)) {
            Write-Host "Failed to start PostgreSQL" -ForegroundColor Red
            exit 1
        }
        Write-Host "PostgreSQL started" -ForegroundColor Green
    }

    # Start Redis
    if ($servicesToStart -contains "redis") {
        Write-Host "Starting Redis..." -ForegroundColor Cyan
        if (-not (Start-Service redis)) {
            Write-Host "Failed to start Redis" -ForegroundColor Red
            exit 1
        }
        Write-Host "Redis started" -ForegroundColor Green
    }

    # Start Ollama (but don't wait for model loading)
    if ($servicesToStart -contains "ollama") {
        Write-Host "Starting Ollama service..." -ForegroundColor Cyan
        if (-not (Start-Service ollama)) {
            Write-Host "Failed to start Ollama" -ForegroundColor Red
            exit 1
        }
        Write-Host "Ollama started (model loading in background)" -ForegroundColor Green
    }

    # Start middleware
    if ($servicesToStart -contains "middleware") {
        Write-Host "Starting Middleware application..." -ForegroundColor Cyan
        $env:SPRING_PROFILES_ACTIVE = "docker"

        if (-not (Start-Service middleware)) {
            Write-Host "Failed to start Middleware" -ForegroundColor Red
            exit 1
        }
        Write-Host "Middleware started" -ForegroundColor Green
    }

    if ($WaitForHealthy) {
        Write-Host "Waiting for core services to be healthy..." -ForegroundColor Yellow
        if (-not (Wait-ForServicesHealthy $servicesToCheck)) {
            Write-Host "Some services failed to become healthy" -ForegroundColor Red
            exit 1
        }
        Write-Host "Core services are healthy" -ForegroundColor Green
    }

    Write-Host "`nApplication Status:" -ForegroundColor Cyan
    Get-ServiceStatus $servicesToCheck

    Write-Host "`nFast startup complete!" -ForegroundColor Green
    Write-Host "API: http://localhost:8080" -ForegroundColor White
    Write-Host "Health: http://localhost:8080/actuator/health" -ForegroundColor White

    if (-not $SkipOllama) {
        Write-Host "`nNote: Ollama model loading happens in the background." -ForegroundColor Yellow
        Write-Host "The model will be ready when you make your first API call." -ForegroundColor Yellow
    }

} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

#!/usr/bin/env pwsh

param(
    [string]$Services = "postgres,redis,embeddings,middleware",
    [string]$Profile = "docker",
    [switch]$SkipEmbeddings,
    [switch]$WaitForHealthy = $true,
    [switch]$Build
)

function Wait-ForUserInput {
    param([string]$Message = "Press any key to continue...")
    Write-Host $Message -ForegroundColor Yellow
    try {
        $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
    } catch {
        Write-Host "Press Enter to continue..."
        Read-Host
    }
}

function Safe-Exit {
    param([int]$ExitCode = 0)
    Write-Host "Script completed with exit code: $ExitCode" -ForegroundColor Cyan
    Wait-ForUserInput "Press any key to exit..."
    exit $ExitCode
}

$ErrorActionPreference = "Continue"

Write-Host "Starting Knowledge-Aware LLM Middleware Application" -ForegroundColor Green
Write-Host "====================================================" -ForegroundColor Green

try {
    Write-Host "Loading common functions..." -ForegroundColor Yellow
    if (Test-Path "$PSScriptRoot\common.ps1") {
        . "$PSScriptRoot\common.ps1"
        Write-Host "Common functions loaded successfully" -ForegroundColor Green
    } else {
        Write-Host "Common functions file not found" -ForegroundColor Red
        Safe-Exit 1
    }

    Write-Host "Checking prerequisites..." -ForegroundColor Yellow
    
    if (-not (Test-DockerRunning)) {
        Write-Host "Docker Desktop is not running" -ForegroundColor Red
        Safe-Exit 1
    }

    if (-not (Test-PortsAvailable)) {
        Write-Host "Required ports are not available" -ForegroundColor Red
        Safe-Exit 1
    }

    Write-Host "Prerequisites check passed" -ForegroundColor Green

    if ($Build) {
        Write-Host "Building application..." -ForegroundColor Yellow
        if (-not (Invoke-BuildApplication)) {
            Write-Host "Build failed" -ForegroundColor Red
            Safe-Exit 1
        }
        Write-Host "Build completed successfully" -ForegroundColor Green
    }

    $servicesToStart = $Services -split "," | ForEach-Object { $_.Trim() }

    if ($SkipEmbeddings) {
        $servicesToStart = $servicesToStart | Where-Object { $_ -ne "embeddings" }
        Write-Host "Skipping embeddings service" -ForegroundColor Yellow
    }

    # Add Ollama to the services list since middleware depends on it
    if (-not ($servicesToStart -contains "ollama")) {
        $servicesToStart += "ollama"
        Write-Host "Adding Ollama service (required for middleware)" -ForegroundColor Yellow
    }

    # Create a list of services to check for health
    $servicesToCheck = $servicesToStart | Where-Object { $_ -ne "embeddings" }

    Write-Host "Starting services..." -ForegroundColor Yellow

    if ($servicesToStart -contains "postgres") {
        Write-Host "Starting PostgreSQL..." -ForegroundColor Cyan
        if (-not (Start-Service postgres)) {
            Write-Host "Failed to start PostgreSQL" -ForegroundColor Red
            Safe-Exit 1
        }
        Write-Host "PostgreSQL started" -ForegroundColor Green
    }

    if ($servicesToStart -contains "redis") {
        Write-Host "Starting Redis..." -ForegroundColor Cyan
        if (-not (Start-Service redis)) {
            Write-Host "Failed to start Redis" -ForegroundColor Red
            Safe-Exit 1
        }
        Write-Host "Redis started" -ForegroundColor Green
    }

    # Embeddings are now handled by the Ollama container
    # No need to start a separate embeddings service

    # Start Ollama first since middleware depends on it
    Write-Host "Starting Ollama service..." -ForegroundColor Cyan
    if (-not (Start-Service ollama)) {
        Write-Host "Failed to start Ollama" -ForegroundColor Red
        Safe-Exit 1
    }
    Write-Host "Ollama started" -ForegroundColor Green
    
    # Wait a moment for Ollama to initialize
    Write-Host "Waiting for Ollama to initialize..." -ForegroundColor Yellow
    Start-Sleep -Seconds 5
    
    if ($servicesToStart -contains "middleware") {
        Write-Host "Starting Middleware application..." -ForegroundColor Cyan
        $env:SPRING_PROFILES_ACTIVE = $Profile
        
        if (-not (Start-Service middleware)) {
            Write-Host "Failed to start Middleware" -ForegroundColor Red
            Safe-Exit 1
        }
        Write-Host "Middleware started" -ForegroundColor Green
    }

    if ($WaitForHealthy) {
        Write-Host "Waiting for services to be healthy..." -ForegroundColor Yellow
        if (-not (Wait-ForServicesHealthy $servicesToCheck)) {
            Write-Host "Some services failed to become healthy" -ForegroundColor Red
            Safe-Exit 1
        }
        Write-Host "All services are healthy" -ForegroundColor Green
    }

    Write-Host "Application Status:" -ForegroundColor Cyan
    Get-ServiceStatus $servicesToCheck

    Write-Host "Application started successfully!" -ForegroundColor Green
    Write-Host "API: http://localhost:8080" -ForegroundColor White
    Write-Host "Health: http://localhost:8080/actuator/health" -ForegroundColor White

    Safe-Exit 0

} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    Safe-Exit 1
}

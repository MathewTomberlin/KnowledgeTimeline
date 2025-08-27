#!/usr/bin/env pwsh

param(
    [string]$Model = "llama2",
    [switch]$Force
)

Write-Host "Knowledge-Aware LLM Middleware - Model Pre-puller" -ForegroundColor Green
Write-Host "======================================================" -ForegroundColor Green

try {
    # Load common functions
    if (Test-Path "$PSScriptRoot\common.ps1") {
        . "$PSScriptRoot\common.ps1"
    } else {
        Write-Host "Common functions file not found" -ForegroundColor Red
        exit 1
    }

    # Check if Docker is running
    if (-not (Test-DockerRunning)) {
        Write-Host "Docker Desktop is not running" -ForegroundColor Red
        exit 1
    }

    # Start Ollama service if not running
    Write-Host "Ensuring Ollama service is running..." -ForegroundColor Yellow
    $ollamaRunning = docker-compose ps ollama --format "table {{.Status}}" 2>$null
    if ($ollamaRunning -notmatch "Up") {
        Write-Host "Starting Ollama service..." -ForegroundColor Cyan
        if (-not (Start-Service ollama)) {
            Write-Host "Failed to start Ollama" -ForegroundColor Red
            exit 1
        }
        Write-Host "Waiting for Ollama to be ready..." -ForegroundColor Yellow
        Start-Sleep -Seconds 10
    }

    # Check current models
    Write-Host "Checking current models..." -ForegroundColor Yellow
    try {
        $currentModels = docker-compose exec -T ollama ollama list 2>$null
        Write-Host "Current models:" -ForegroundColor Cyan
        Write-Host $currentModels
    } catch {
        Write-Host "Could not list current models" -ForegroundColor Yellow
    }

    # Check if model already exists
    if (-not $Force) {
        try {
            $modelCheck = docker-compose exec -T ollama ollama list 2>$null
            if ($modelCheck -match $Model) {
                Write-Host "Model '$Model' is already available!" -ForegroundColor Green
                Write-Host "Use -Force to re-download if needed." -ForegroundColor Yellow
                exit 0
            }
        } catch {
            Write-Host "Could not check model status" -ForegroundColor Yellow
        }
    }

    # Pull the model
    Write-Host "Pulling model '$Model' (this may take several minutes)..." -ForegroundColor Cyan
    $startTime = Get-Date

    try {
        docker-compose exec -T ollama ollama pull $Model
        $endTime = Get-Date
        $duration = $endTime - $startTime
        Write-Host "Model '$Model' downloaded successfully in $($duration.TotalMinutes.ToString("F1")) minutes!" -ForegroundColor Green
    } catch {
        Write-Host "Failed to pull model '$Model'" -ForegroundColor Red
        exit 1
    }

    # Verify the model is available
    Write-Host "Verifying model availability..." -ForegroundColor Yellow
    try {
        $verifyModels = docker-compose exec -T ollama ollama list 2>$null
        if ($verifyModels -match $Model) {
            Write-Host "✓ Model '$Model' is ready to use!" -ForegroundColor Green
        } else {
            Write-Host "⚠ Model '$Model' may not be properly installed" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "Could not verify model installation" -ForegroundColor Yellow
    }

    Write-Host "`nModel preparation complete!" -ForegroundColor Green
    Write-Host "The model will be available for future application starts." -ForegroundColor White

} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

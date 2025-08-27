#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Restart the Knowledge-Aware LLM Middleware application by stopping and then starting all services.

.DESCRIPTION
    This script combines the functionality of stop-app.ps1 and start-app.ps1 to provide
    a convenient way to restart the entire application stack. It first stops all running
    services gracefully, then starts them back up.

.PARAMETER Services
    Comma-separated list of specific services to restart. Default: all services.
    Valid values: postgres, redis, ollama, middleware

.PARAMETER Profile
    Spring profile to use for the middleware application. Default: docker.

.PARAMETER SkipEmbeddings
    Skip starting the embeddings service (Ollama).

.PARAMETER WaitForHealthy
    Wait for all services to be healthy before completing. Default: true.

.PARAMETER Build
    Build the application before starting.

.PARAMETER Force
    Force stop containers without graceful shutdown.

.PARAMETER RemoveVolumes
    Remove data volumes when stopping (WARNING: This will delete all data).

.EXAMPLE
    .\restart-app.ps1

    Restart all services gracefully with default settings.

.EXAMPLE
    .\restart-app.ps1 -Services middleware,postgres

    Restart only the middleware and postgres services.

.EXAMPLE
    .\restart-app.ps1 -Build -WaitForHealthy

    Build the application and restart all services, waiting for them to be healthy.
#>

param(
    [Parameter()]
    [string]$Services = "postgres,redis,ollama,middleware",

    [Parameter()]
    [string]$Profile = "docker",

    [Parameter()]
    [switch]$SkipEmbeddings,

    [Parameter()]
    [switch]$WaitForHealthy = $true,

    [Parameter()]
    [switch]$Build,

    [Parameter()]
    [switch]$Force,

    [Parameter()]
    [switch]$RemoveVolumes
)

# Function to write colored output
function Write-ColorOutput {
    param(
        [string]$Message,
        [string]$Color = "White",
        [switch]$NoNewline
    )

    if ($NoNewline) {
        Write-Host $Message -ForegroundColor $Color -NoNewline
    } else {
        Write-Host $Message -ForegroundColor $Color
    }
}

# Function to exit with proper code
function Safe-Exit {
    param([int]$ExitCode = 0)
    Write-ColorOutput "Script completed with exit code: $ExitCode" "Cyan"
    exit $ExitCode
}

$ErrorActionPreference = "Continue"

Write-ColorOutput "Restarting Knowledge-Aware LLM Middleware Application" "Green"
Write-ColorOutput "===========================================================" "Green"

try {
    Write-ColorOutput "Loading common functions..." "Yellow"
    if (Test-Path "$PSScriptRoot\common.ps1") {
        . "$PSScriptRoot\common.ps1"
        Write-ColorOutput "Common functions loaded successfully" "Green"
    } else {
        Write-ColorOutput "Common functions file not found" "Red"
        Safe-Exit 1
    }

    # Parse services to restart
    $servicesToRestart = $Services -split "," | ForEach-Object { $_.Trim() }

    # Step 1: Stop the application
    Write-ColorOutput "`nStep 1: Stopping services..." "Yellow"

    # Check if Docker is running
    if (-not (Test-DockerRunning)) {
        Write-ColorOutput "Docker Desktop is not running. Nothing to stop." "Red"
        Safe-Exit 1
    }

    # Stop services in reverse dependency order
    if ($servicesToRestart -contains "middleware") {
        Write-ColorOutput "   Stopping Middleware application..." "Cyan"
        Stop-Service middleware
    }

    if ($servicesToRestart -contains "ollama") {
        Write-ColorOutput "   Stopping Ollama service..." "Cyan"
        Stop-Service ollama
    }

    if ($servicesToRestart -contains "redis") {
        Write-ColorOutput "   Stopping Redis..." "Cyan"
        Stop-Service redis
    }

    if ($servicesToRestart -contains "postgres") {
        Write-ColorOutput "   Stopping PostgreSQL..." "Cyan"
        Stop-Service postgres
    }

    # Remove containers and volumes if requested
    if ($RemoveVolumes) {
        Write-ColorOutput "`nRemoving containers and volumes..." "Red"

        try {
            $result = docker-compose down -v 2>&1

            if ($LASTEXITCODE -eq 0) {
                Write-ColorOutput "All containers and volumes removed" "Green"
            } else {
                Write-ColorOutput "Some containers or volumes may not have been removed" "Yellow"
                Write-ColorOutput "   Error: $result" "Yellow"
            }
        } catch {
            Write-ColorOutput "Error removing containers and volumes: $($_.Exception.Message)" "Red"
        }
    } else {
        # Just stop containers
        try {
            $result = docker-compose stop $servicesToRestart 2>&1

            if ($LASTEXITCODE -eq 0) {
                Write-ColorOutput "All requested services stopped" "Green"
            } else {
                Write-ColorOutput "Some services may not have stopped properly" "Yellow"
                Write-ColorOutput "   Error: $result" "Yellow"
            }
        } catch {
            Write-ColorOutput "Error stopping services: $($_.Exception.Message)" "Red"
        }
    }

    Write-ColorOutput "`nApplication stopped successfully!" "Green"

    # Step 2: Start the application
    Write-ColorOutput "`nStep 2: Starting services..." "Yellow"

    if ($Build) {
        Write-ColorOutput "Building application..." "Yellow"
        if (-not (Invoke-BuildApplication)) {
            Write-ColorOutput "Build failed" "Red"
            Safe-Exit 1
        }
        Write-ColorOutput "Build completed successfully" "Green"
    }

    if ($SkipEmbeddings) {
        $servicesToRestart = $servicesToRestart | Where-Object { $_ -ne "embeddings" }
        Write-ColorOutput "Skipping embeddings service" "Yellow"
    }

    # Add Ollama to the services list since middleware depends on it
    if (-not ($servicesToRestart -contains "ollama")) {
        $servicesToRestart += "ollama"
        Write-ColorOutput "Adding Ollama service (required for middleware)" "Yellow"
    }

    # Create a list of services to check for health
    $servicesToCheck = $servicesToRestart | Where-Object { $_ -ne "embeddings" }

    if ($servicesToRestart -contains "postgres") {
        Write-ColorOutput "Starting PostgreSQL..." "Cyan"
        if (-not (Start-Service postgres)) {
            Write-ColorOutput "Failed to start PostgreSQL" "Red"
            Safe-Exit 1
        }
        Write-ColorOutput "PostgreSQL started" "Green"
    }

    if ($servicesToRestart -contains "redis") {
        Write-ColorOutput "Starting Redis..." "Cyan"
        if (-not (Start-Service redis)) {
            Write-ColorOutput "Failed to start Redis" "Red"
            Safe-Exit 1
        }
        Write-ColorOutput "Redis started" "Green"
    }

    # Start Ollama first since middleware depends on it
    Write-ColorOutput "Starting Ollama service..." "Cyan"
    if (-not (Start-Service ollama)) {
        Write-ColorOutput "Failed to start Ollama" "Red"
        Safe-Exit 1
    }
    Write-ColorOutput "Ollama started" "Green"

    # Check if llama2 model is available, if not pull it
    Write-ColorOutput "Checking Ollama model availability..." "Yellow"
    try {
        $modelCheck = docker-compose exec -T ollama ollama list 2>$null
        if ($modelCheck -notmatch "llama2") {
            Write-ColorOutput "Pulling llama2 model (this may take a few minutes)..." "Yellow"
            docker-compose exec -T ollama ollama pull llama2
        } else {
            Write-ColorOutput "llama2 model already available" "Green"
        }
    } catch {
        Write-ColorOutput "Could not check model status, proceeding..." "Yellow"
    }

    # Wait a moment for Ollama to initialize
    Write-ColorOutput "Waiting for Ollama to initialize..." "Yellow"
    Start-Sleep -Seconds 5

    if ($servicesToRestart -contains "middleware") {
        Write-ColorOutput "Starting Middleware application..." "Cyan"
        $env:SPRING_PROFILES_ACTIVE = $Profile

        if (-not (Start-Service middleware)) {
            Write-ColorOutput "Failed to start Middleware" "Red"
            Safe-Exit 1
        }
        Write-ColorOutput "Middleware started" "Green"
    }

    if ($WaitForHealthy) {
        Write-ColorOutput "Waiting for services to be healthy..." "Yellow"
        if (-not (Wait-ForServicesHealthy $servicesToCheck)) {
            Write-ColorOutput "Some services failed to become healthy" "Red"
            Safe-Exit 1
        }
        Write-ColorOutput "All services are healthy" "Green"
    }

    Write-ColorOutput "`nApplication Status:" "Cyan"
    Get-ServiceStatus $servicesToCheck

    Write-ColorOutput "`nApplication restarted successfully!" "Green"
    Write-ColorOutput "API: http://localhost:8080" "White"
    Write-ColorOutput "Health: http://localhost:8080/actuator/health" "White"

    Safe-Exit 0

} catch {
    Write-ColorOutput "Error: $($_.Exception.Message)" "Red"
    Safe-Exit 1
}
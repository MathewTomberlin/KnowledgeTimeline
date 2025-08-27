#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Reset the Knowledge-Aware LLM Middleware database and run fresh migrations.

.DESCRIPTION
    This script completely resets the database:
    - Stops all services
    - Removes all data volumes
    - Starts fresh database
    - Runs Flyway migrations
    - Creates test data
    
    WARNING: This will permanently delete all data!

.PARAMETER Confirm
    Skip confirmation prompt (use with caution).

.PARAMETER SkipTestData
    Skip creating test data after reset.

.PARAMETER Profile
    Spring profile to use after reset. Default: docker.

.EXAMPLE
    .\db-reset.ps1
    
    Reset database with confirmation prompt.

.EXAMPLE
    .\db-reset.ps1 -Confirm
    
    Reset database without confirmation (DANGEROUS!).

.EXAMPLE
    .\db-reset.ps1 -SkipTestData -Profile local
    
    Reset database without test data, use local profile.
#>

param(
    [Parameter()]
    [switch]$Confirm,
    
    [Parameter()]
    [switch]$SkipTestData,
    
    [Parameter()]
    [ValidateSet("docker", "local", "test")]
    [string]$Profile = "docker"
)

# Import common functions
. "$PSScriptRoot\common.ps1"

Write-Host "Knowledge-Aware LLM Middleware - Database Reset" -ForegroundColor Red
Write-Host "==================================================" -ForegroundColor Red

# Check if Docker is running
if (-not (Test-DockerRunning)) {
    Write-Host "Docker Desktop is not running. Please start Docker Desktop first." -ForegroundColor Red
    exit 1
}

# Confirmation prompt
if (-not $Confirm) {
    Write-Host "`nWARNING: This will permanently delete ALL data!" -ForegroundColor Red
    Write-Host "   This includes:" -ForegroundColor Red
    Write-Host "   - All database records" -ForegroundColor Red
    Write-Host "   - All Redis cache data" -ForegroundColor Red
    Write-Host "   - All application data" -ForegroundColor Red
    Write-Host "   - All user sessions and memories" -ForegroundColor Red
    
    Write-Host "`nThis is useful for:" -ForegroundColor Yellow
    Write-Host "   - Fixing migration issues" -ForegroundColor Yellow
    Write-Host "   - Starting fresh development" -ForegroundColor Yellow
    Write-Host "   - Resolving data corruption" -ForegroundColor Yellow
    
    $confirmation = Read-Host "`nAre you sure you want to continue? (Type 'YES' to confirm)"
    if ($confirmation -ne "YES") {
        Write-Host "Database reset cancelled by user" -ForegroundColor Yellow
        exit 0
    }
    
    Write-Host "Confirmed. Proceeding with database reset..." -ForegroundColor Red
} else {
    Write-Host "Skipping confirmation prompt (use with caution)" -ForegroundColor Yellow
}

Write-Host "`nStarting database reset process..." -ForegroundColor Yellow

# Step 1: Stop all services
Write-Host "`nStep 1: Stopping all services..." -ForegroundColor Red
try {
    $result = docker-compose down -v 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "All services stopped and volumes removed" -ForegroundColor Green
    } else {
        Write-Host "Some services may not have stopped properly" -ForegroundColor Yellow
        Write-Host "   Error: $result" -ForegroundColor Yellow
    }
} catch {
    Write-Host "Error stopping services: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 2: Start fresh database
Write-Host "`nStep 2: Starting fresh database..." -ForegroundColor Green
try {
    $result = docker-compose up -d postgres 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "PostgreSQL started successfully" -ForegroundColor Green
    } else {
        Write-Host "Failed to start PostgreSQL" -ForegroundColor Red
        Write-Host "   Error: $result" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "Error starting PostgreSQL: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 3: Wait for database to be ready
Write-Host "`nStep 3: Waiting for database to be ready..." -ForegroundColor Yellow
$maxWaitTime = 60 # 1 minute
$startTime = Get-Date
$dbReady = $false

while (-not $dbReady -and ((Get-Date) - $startTime).TotalSeconds -lt $maxWaitTime) {
    try {
        $connection = Test-NetConnection -ComputerName localhost -Port 5432 -WarningAction SilentlyContinue 2>$null
        if ($connection.TcpTestSucceeded) {
            $dbReady = $true
            Write-Host "Database is ready" -ForegroundColor Green
        } else {
            Write-Host "   Waiting for database... ($([math]::Round(((Get-Date) - $startTime).TotalSeconds))s)" -ForegroundColor Yellow
            Start-Sleep -Seconds 5
        }
    } catch {
        Write-Host "   Waiting for database... ($([math]::Round(((Get-Date) - $startTime).TotalSeconds))s)" -ForegroundColor Yellow
        Start-Sleep -Seconds 5
    }
}

if (-not $dbReady) {
    Write-Host "Database failed to become ready within timeout" -ForegroundColor Red
    exit 1
}

# Step 4: Start Redis
Write-Host "`nStep 4: Starting Redis..." -ForegroundColor Green
try {
    $result = docker-compose up -d redis 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Redis started successfully" -ForegroundColor Green
    } else {
        Write-Host "Failed to start Redis" -ForegroundColor Red
        Write-Host "   Error: $result" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "Error starting Redis: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 5: Start Ollama
Write-Host "`nStep 5: Starting Ollama..." -ForegroundColor Green
try {
    $result = docker-compose up -d ollama 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Ollama started successfully" -ForegroundColor Green
    } else {
        Write-Host "Failed to start Ollama" -ForegroundColor Red
        Write-Host "   Error: $result" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "Error starting Ollama: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 6: Start middleware to run migrations
Write-Host "`nStep 6: Starting middleware to run migrations..." -ForegroundColor Green

# Set environment variables
$env:SPRING_PROFILES_ACTIVE = $Profile

try {
    $result = docker-compose up -d middleware 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Middleware started successfully" -ForegroundColor Green
    } else {
        Write-Host "Failed to start middleware" -ForegroundColor Red
        Write-Host "   Error: $result" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "Error starting middleware: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Step 7: Wait for migrations to complete
Write-Host "`nStep 7: Waiting for migrations to complete..." -ForegroundColor Yellow
$maxWaitTime = 120 # 2 minutes
$startTime = Get-Date
$migrationsComplete = $false

while (-not $migrationsComplete -and ((Get-Date) - $startTime).TotalSeconds -lt $maxWaitTime) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing -TimeoutSec 10 2>$null
        
        if ($response.StatusCode -eq 200) {
            $migrationsComplete = $true
            Write-Host "Migrations completed successfully" -ForegroundColor Green
        } else {
            Write-Host "   Waiting for migrations... ($([math]::Round(((Get-Date) - $startTime).TotalSeconds))s)" -ForegroundColor Yellow
            Start-Sleep -Seconds 10
        }
    } catch {
        Write-Host "   Waiting for migrations... ($([math]::Round(((Get-Date) - $startTime).TotalSeconds))s)" -ForegroundColor Yellow
        Start-Sleep -Seconds 10
    }
}

if (-not $migrationsComplete) {
    Write-Host "Migrations failed to complete within timeout" -ForegroundColor Red
    Write-Host "   Check logs: .\scripts\logs.ps1 -Service middleware" -ForegroundColor Yellow
    exit 1
}

# Step 8: Create test data (if not skipped)
if (-not $SkipTestData) {
    Write-Host "`nStep 8: Creating test data..." -ForegroundColor Green
    
    # Wait a bit more for TestDataCreator to run
    Start-Sleep -Seconds 10
    
    Write-Host "Test data creation initiated" -ForegroundColor Green
    Write-Host "   Check logs for TestDataCreator output" -ForegroundColor White
} else {
    Write-Host "`nStep 8: Skipping test data creation" -ForegroundColor Yellow
}

# Final status
Write-Host "`nFinal Status:" -ForegroundColor Cyan
Get-ServiceStatus @("postgres", "redis", "ollama", "middleware")

Write-Host "`nDatabase reset completed successfully!" -ForegroundColor Green
Write-Host "API available at: http://localhost:8080" -ForegroundColor White
Write-Host "Health check: http://localhost:8080/actuator/health" -ForegroundColor White

if (-not $SkipTestData) {
    Write-Host "Test data should be available" -ForegroundColor White
    Write-Host "   Check logs for TestDataCreator confirmation" -ForegroundColor White
}

Write-Host "`nUseful Commands:" -ForegroundColor Cyan
Write-Host "   Check status: .\scripts\status.ps1" -ForegroundColor White
Write-Host "   View logs: .\scripts\logs.ps1 -Service middleware" -ForegroundColor White
Write-Host "   Test API: .\scripts\test-api.ps1" -ForegroundColor White
Write-Host "   End-to-end test: .\scripts\test-end-to-end.ps1" -ForegroundColor White

#!/usr/bin/env pwsh
<#
.SYNOPSIS
    View logs for Knowledge-Aware LLM Middleware services.

.DESCRIPTION
    This script displays logs for the specified service(s):
    - PostgreSQL logs
    - Redis logs
    - Embeddings service logs
    - Middleware application logs
    
    Logs can be viewed in real-time or as a snapshot.

.PARAMETER Service
    Service to view logs for. Default: middleware.
    Valid values: postgres, redis, embeddings, middleware, all

.PARAMETER Lines
    Number of log lines to display. Default: 50.

.PARAMETER Follow
    Follow logs in real-time (like tail -f).

.PARAMETER Since
    Show logs since timestamp (e.g., "10m", "2h", "2023-01-01T10:00:00").

.PARAMETER Until
    Show logs until timestamp.

.PARAMETER Filter
    Filter logs by text pattern.

.EXAMPLE
    .\logs.ps1
    
    Show last 50 lines of middleware logs.

.EXAMPLE
    .\logs.ps1 -Service postgres -Lines 100
    
    Show last 100 lines of PostgreSQL logs.

.EXAMPLE
    .\logs.ps1 -Service middleware -Follow
    
    Follow middleware logs in real-time.

.EXAMPLE
    .\logs.ps1 -Service all -Filter "ERROR"
    
    Show all services' logs filtered for ERROR messages.

.EXAMPLE
    .\logs.ps1 -Service middleware -Since "10m"
    
    Show middleware logs from the last 10 minutes.
#>

param(
    [Parameter()]
    [ValidateSet("postgres", "redis", "ollama", "middleware", "all")]
    [string]$Service = "middleware",
    
    [Parameter()]
    [int]$Lines = 50,
    
    [Parameter()]
    [switch]$Follow,
    
    [Parameter()]
    [string]$Since,
    
    [Parameter()]
    [string]$Until,
    
    [Parameter()]
    [string]$Filter
)

# Import common functions
. "$PSScriptRoot\common.ps1"

Write-Host "Knowledge-Aware LLM Middleware - Service Logs" -ForegroundColor Green
Write-Host "===============================================" -ForegroundColor Green

# Check if Docker is running
if (-not (Test-DockerRunning)) {
    Write-Host "Docker Desktop is not running" -ForegroundColor Red
    exit 1
}

# Determine which services to show logs for
$servicesToLog = @()
if ($Service -eq "all") {
    $servicesToLog = @("postgres", "redis", "ollama", "middleware")
} else {
    $servicesToLog = @($Service)
}

# Build docker-compose logs command
$logParams = @()
$logParams += "--tail $Lines"

if ($Follow) {
    $logParams += "-f"
}

if ($Since) {
    $logParams += "--since `"$Since`""
}

if ($Until) {
    $logParams += "--until `"$Until`""
}

$logCommand = "docker-compose logs $($logParams -join ' ')"

# Function to display logs for a service
function Show-ServiceLogs {
    param([string]$ServiceName)
    
    Write-Host "`nLogs for $($ServiceName):" -ForegroundColor Cyan
    Write-Host "=========================" -ForegroundColor Cyan
    
    try {
        # Build service-specific log command
        $serviceLogCommand = "$logCommand $ServiceName"
        
        if ($Filter) {
            # If filtering, we need to get logs and then filter them
            $logs = Invoke-Expression $serviceLogCommand 2>$null
            
            if ($logs) {
                $filteredLogs = $logs | Where-Object { $_ -match $Filter }
                
                if ($filteredLogs) {
                    # Color-code log levels
                    $filteredLogs | ForEach-Object {
                        $line = $_
                        if ($line -match "ERROR|Exception|Failed|FATAL") {
                            Write-Host $line -ForegroundColor Red
                        } elseif ($line -match "WARN|Warning") {
                            Write-Host $line -ForegroundColor Yellow
                        } elseif ($line -match "INFO|Started|Completed|SUCCESS") {
                            Write-Host $line -ForegroundColor Green
                        } elseif ($line -match "DEBUG") {
                            Write-Host $line -ForegroundColor Gray
                        } else {
                            Write-Host $line -ForegroundColor White
                        }
                    }
                } else {
                    Write-Host "   No logs matching filter '$Filter'" -ForegroundColor Yellow
                }
            } else {
                Write-Host "   No logs available" -ForegroundColor Gray
            }
        } else {
            # No filter, show all logs
            if ($Follow) {
                Write-Host "   Following logs in real-time... (Press Ctrl+C to stop)" -ForegroundColor Yellow
                Write-Host ""
                Invoke-Expression $serviceLogCommand
            } else {
                $logs = Invoke-Expression $serviceLogCommand 2>$null
                
                if ($logs) {
                    # Color-code log levels
                    $logs | ForEach-Object {
                        $line = $_
                        if ($line -match "ERROR|Exception|Failed|FATAL") {
                            Write-Host $line -ForegroundColor Red
                        } elseif ($line -match "WARN|Warning") {
                            Write-Host $line -ForegroundColor Yellow
                        } elseif ($line -match "INFO|Started|Completed|SUCCESS") {
                            Write-Host $line -ForegroundColor Green
                        } elseif ($line -match "DEBUG") {
                            Write-Host $line -ForegroundColor Gray
                        } else {
                            Write-Host $line -ForegroundColor White
                        }
                    }
                } else {
                    Write-Host "   No logs available" -ForegroundColor Gray
                }
            }
        }
        
    } catch {
        Write-Host "   Error getting logs: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Show logs for each service
foreach ($service in $servicesToLog) {
    Show-ServiceLogs $service
}

# Show summary
Write-Host "`nLog Summary:" -ForegroundColor Cyan
Write-Host "===============" -ForegroundColor Cyan
Write-Host "   Service(s): $($servicesToLog -join ', ')" -ForegroundColor White
Write-Host "   Lines: $Lines" -ForegroundColor White
Write-Host "   Follow: $(if ($Follow) { 'Yes' } else { 'No' })" -ForegroundColor White
if ($Since) { Write-Host "   Since: $Since" -ForegroundColor White }
if ($Until) { Write-Host "   Until: $Until" -ForegroundColor White }
if ($Filter) { Write-Host "   Filter: $Filter" -ForegroundColor White }

Write-Host "`nUseful Commands:" -ForegroundColor Cyan
Write-Host "   View all logs: .\scripts\logs.ps1 -Service all" -ForegroundColor White
Write-Host "   Follow logs: .\scripts\logs.ps1 -Service middleware -Follow" -ForegroundColor White
Write-Host "   Filter errors: .\scripts\logs.ps1 -Service all -Filter 'ERROR'" -ForegroundColor White
Write-Host "   Recent logs: .\scripts\logs.ps1 -Service postgres -Lines 100" -ForegroundColor White
Write-Host "   Time-based: .\scripts\logs.ps1 -Service middleware -Since '1h'" -ForegroundColor White

if (-not $Follow) {
    Write-Host "`nLog viewing completed" -ForegroundColor Green
}

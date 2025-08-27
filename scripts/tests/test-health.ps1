#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Test the health endpoint of the KnowledgeTimeline application
.DESCRIPTION
    Tests the /actuator/health endpoint to verify the application is running
.PARAMETER BaseUrl
    Base URL of the application (default: http://localhost:8080)
.EXAMPLE
    .\test-health.ps1
.EXAMPLE
    .\test-health.ps1 -BaseUrl "http://localhost:9090"
#>

param(
    [string]$BaseUrl = "http://localhost:8080"
)

Write-Host "🩺 Testing Health Endpoint" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan

$healthUrl = "$BaseUrl/actuator/health"

try {
    Write-Host "Testing: $healthUrl" -ForegroundColor Yellow

    $response = Invoke-WebRequest -Uri $healthUrl -Method GET -TimeoutSec 10

    if ($response.StatusCode -eq 200) {
        $content = $response.Content | ConvertFrom-Json
        Write-Host "✅ Health Check PASSED" -ForegroundColor Green
        Write-Host "   Status: $($content.status)" -ForegroundColor Green
        $responseTime = if ($response.Headers.ContainsKey('X-Response-Time')) { $response.Headers.'X-Response-Time' } else { 'N/A' }
        Write-Host "   Response Time: $responseTime" -ForegroundColor Green
        return $true
    } else {
        Write-Host "❌ Health Check FAILED - Unexpected status: $($response.StatusCode)" -ForegroundColor Red
        return $false
    }
}
catch {
    Write-Host "❌ Health Check FAILED - Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   Make sure the application is running and accessible at $BaseUrl" -ForegroundColor Yellow
    return $false
}

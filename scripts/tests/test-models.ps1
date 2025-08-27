#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Test the models endpoint of the KnowledgeTimeline application
.DESCRIPTION
    Tests the /v1/models endpoint to verify available models
.PARAMETER BaseUrl
    Base URL of the application (default: http://localhost:8080)
.PARAMETER ApiKey
    API key for authentication (default: test-api-key-123)
.EXAMPLE
    .\test-models.ps1
.EXAMPLE
    .\test-models.ps1 -ApiKey "your-api-key"
#>

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$ApiKey = "test-api-key-123"
)

Write-Host "🤖 Testing Models Endpoint" -ForegroundColor Cyan
Write-Host "=================================" -ForegroundColor Cyan

$modelsUrl = "$BaseUrl/v1/models"
$headers = @{
    "Authorization" = "Bearer $ApiKey"
}

try {
    Write-Host "Testing: $modelsUrl" -ForegroundColor Yellow
    Write-Host "Using API Key: $ApiKey" -ForegroundColor Gray

    $response = Invoke-WebRequest -Uri $modelsUrl -Method GET -Headers $headers -TimeoutSec 15

    if ($response.StatusCode -eq 200) {
        $content = $response.Content | ConvertFrom-Json
        $modelCount = $content.data.Count

        Write-Host "✅ Models Endpoint PASSED" -ForegroundColor Green
        Write-Host "   Found $modelCount model(s):" -ForegroundColor Green

        foreach ($model in $content.data) {
            Write-Host "   - $($model.id) (owned by $($model.owned_by))" -ForegroundColor Green
        }

        return $true
    } else {
        Write-Host "❌ Models Endpoint FAILED - Unexpected status: $($response.StatusCode)" -ForegroundColor Red
        return $false
    }
}
catch {
    Write-Host "❌ Models Endpoint FAILED - Error: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Message -match "403") {
        Write-Host "   Check your API key: $ApiKey" -ForegroundColor Yellow
    } elseif ($_.Exception.Message -match "Connection") {
        Write-Host "   Make sure the application is running and accessible at $BaseUrl" -ForegroundColor Yellow
    }
    return $false
}

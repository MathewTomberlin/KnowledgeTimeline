#!/usr/bin/env pwsh

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$ApiKey = "test-api-key-123",
    [string]$Title = "Test Document",
    [string]$Content = "This is a test document for knowledge creation",
    [string]$Type = "TURN"
)

Write-Host "Testing Knowledge Object Creation" -ForegroundColor Cyan

$knowledgeUrl = "$BaseUrl/v1/knowledge/objects"
$headers = @{
    "Authorization" = "Bearer $ApiKey"
    "Content-Type" = "application/json"
}

$body = @{
    type = $Type
    title = $Title
    content = $Content
    tenantId = "test-tenant-001"
} | ConvertTo-Json

try {
    Write-Host "Testing: $knowledgeUrl" -ForegroundColor Yellow

    $response = Invoke-WebRequest -Uri $knowledgeUrl -Method POST -Headers $headers -Body $body -TimeoutSec 15

    if ($response.StatusCode -eq 200) {
        $content = $response.Content | ConvertFrom-Json

        Write-Host "PASSED" -ForegroundColor Green
        Write-Host "Object ID: $($content.id)" -ForegroundColor Green
        return $true
    } else {
        Write-Host "FAILED - Status: $($response.StatusCode)" -ForegroundColor Red
        return $false
    }
}
catch {
    Write-Host "FAILED - Error: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Message -match "403") {
        Write-Host "   Check your API key: $ApiKey" -ForegroundColor Yellow
    } elseif ($_.Exception.Message -match "Connection") {
        Write-Host "   Make sure the application is running and accessible at $BaseUrl" -ForegroundColor Yellow
    } elseif ($_.Exception.Message -match "500") {
        Write-Host "   Server error occurred. Check application logs for details." -ForegroundColor Yellow
    }
    # Return false but don't exit the script
    $false
    return
}

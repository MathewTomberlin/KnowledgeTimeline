#!/usr/bin/env pwsh

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$ApiKey = "test-api-key-123",
    [string]$Text = "Hello world"
)

Write-Host "Testing Embeddings Endpoint" -ForegroundColor Cyan

$embeddingsUrl = "$BaseUrl/v1/embeddings"
$headers = @{
    "Authorization" = "Bearer $ApiKey"
    "Content-Type" = "application/json"
}

$body = @{
    input = $Text
} | ConvertTo-Json

try {
    Write-Host "Testing: $embeddingsUrl" -ForegroundColor Yellow

    $response = Invoke-WebRequest -Uri $embeddingsUrl -Method POST -Headers $headers -Body $body -TimeoutSec 30

    if ($response.StatusCode -eq 200) {
        $content = $response.Content | ConvertFrom-Json
        $embeddingCount = $content.data[0].embedding.Count

        Write-Host "PASSED" -ForegroundColor Green
        Write-Host "Embedding Dimensions: $embeddingCount" -ForegroundColor Green

        if ($content.usage) {
            Write-Host "Tokens Used: $($content.usage.total_tokens)" -ForegroundColor Green
        }

        return $true
    } else {
        Write-Host "FAILED - Status: $($response.StatusCode)" -ForegroundColor Red
        return $false
    }
}
catch {
    Write-Host "FAILED - Error: $($_.Exception.Message)" -ForegroundColor Red
    return $false
}

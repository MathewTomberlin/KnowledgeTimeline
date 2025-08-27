#!/usr/bin/env pwsh

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$ApiKey = "test-api-key-123"
)

Write-Host "KnowledgeTimeline Test Suite" -ForegroundColor Cyan
Write-Host "============================" -ForegroundColor Cyan

$results = @()

# Test 1: Health Check
Write-Host "1. Testing Health..." -ForegroundColor Yellow
$result1 = & .\test-health.ps1 -BaseUrl $BaseUrl
$results += @{Name="Health"; Result=$result1}

# Test 2: Models
Write-Host "2. Testing Models..." -ForegroundColor Yellow
$result2 = & .\test-models.ps1 -BaseUrl $BaseUrl -ApiKey $ApiKey
$results += @{Name="Models"; Result=$result2}

# Test 3: Chat
Write-Host "3. Testing Chat..." -ForegroundColor Yellow
$result3 = & .\test-chat.ps1 -BaseUrl $BaseUrl -ApiKey $ApiKey
$results += @{Name="Chat"; Result=$result3}

# Test 4: Knowledge Creation
Write-Host "4. Testing Knowledge Creation..." -ForegroundColor Yellow
$result4 = & .\test-knowledge-create.ps1 -BaseUrl $BaseUrl -ApiKey $ApiKey
$results += @{Name="Knowledge"; Result=$result4}

# Test 5: Embeddings
Write-Host "5. Testing Embeddings..." -ForegroundColor Yellow
$result5 = & .\test-embeddings.ps1 -BaseUrl $BaseUrl -ApiKey $ApiKey
$results += @{Name="Embeddings"; Result=$result5}

# Summary
Write-Host ""
Write-Host "SUMMARY" -ForegroundColor Cyan
Write-Host "=======" -ForegroundColor Cyan

$passed = 0
$total = $results.Count

foreach ($result in $results) {
    if ($result.Result) {
        Write-Host "✅ $($result.Name) - PASSED" -ForegroundColor Green
        $passed++
    } else {
        Write-Host "❌ $($result.Name) - FAILED" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Results: $passed/$total tests passed" -ForegroundColor White

if ($passed -eq $total) {
    Write-Host "🎉 All tests passed!" -ForegroundColor Green
    exit 0
} else {
    Write-Host "⚠️  Some tests failed" -ForegroundColor Yellow
    exit 1
}

#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Test the chat completions endpoint of the KnowledgeTimeline application
.DESCRIPTION
    Tests the /v1/chat/completions endpoint to verify chat functionality
.PARAMETER BaseUrl
    Base URL of the application (default: http://localhost:8080)
.PARAMETER ApiKey
    API key for authentication (default: test-api-key-123)
.PARAMETER Message
    Message to send to the chat (default: "Hello, how are you?")
.PARAMETER Model
    Model to use for chat (default: llama2)
.EXAMPLE
    .\test-chat.ps1
.EXAMPLE
    .\test-chat.ps1 -Message "What is machine learning?"
.EXAMPLE
    .\test-chat.ps1 -ApiKey "your-api-key" -Model "gpt-4"
#>

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$ApiKey = "test-api-key-123",
    [string]$Message = "Hello, how are you?",
    [string]$Model = "llama2"
)

Write-Host "💬 Testing Chat Completions Endpoint" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

$chatUrl = "$BaseUrl/v1/chat/completions"
$headers = @{
    "Authorization" = "Bearer $ApiKey"
    "Content-Type" = "application/json"
}

$body = @{
    model = $Model
    messages = @(
        @{
            role = "user"
            content = $Message
        }
    )
} | ConvertTo-Json

try {
    Write-Host "Testing: $chatUrl" -ForegroundColor Yellow
    Write-Host "Using API Key: $ApiKey" -ForegroundColor Gray
    Write-Host "Message: $Message" -ForegroundColor Gray
    Write-Host "Model: $Model" -ForegroundColor Gray

    $response = Invoke-WebRequest -Uri $chatUrl -Method POST -Headers $headers -Body $body -TimeoutSec 30

    if ($response.StatusCode -eq 200) {
        $content = $response.Content | ConvertFrom-Json

        Write-Host "✅ Chat Completions PASSED" -ForegroundColor Green
        Write-Host "   Chat ID: $($content.id)" -ForegroundColor Green
        Write-Host "   Model: $($content.model)" -ForegroundColor Green
        Write-Host "   Choices: $($content.choices.Count)" -ForegroundColor Green

        if ($content.choices.Count -gt 0) {
            $assistantMessage = $content.choices[0].message.content
            Write-Host "   Response: $($assistantMessage.Substring(0, [Math]::Min(100, $assistantMessage.Length)))..." -ForegroundColor Green
        }

        if ($content.usage) {
            Write-Host "   Tokens Used - Prompt: $($content.usage.prompt_tokens), Completion: $($content.usage.completion_tokens)" -ForegroundColor Green
        }

        return $true
    } else {
        Write-Host "❌ Chat Completions FAILED - Unexpected status: $($response.StatusCode)" -ForegroundColor Red
        return $false
    }
}
catch {
    Write-Host "❌ Chat Completions FAILED - Error: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Message -match "403") {
        Write-Host "   Check your API key: $ApiKey" -ForegroundColor Yellow
    } elseif ($_.Exception.Message -match "Connection") {
        Write-Host "   Make sure the application is running and accessible at $BaseUrl" -ForegroundColor Yellow
    }
    return $false
}

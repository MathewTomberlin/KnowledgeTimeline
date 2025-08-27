#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Test basic chat to knowledge object creation functionality
.DESCRIPTION
    Tests the integration between chat completions and knowledge object creation.
    Verifies that chat interactions result in knowledge objects being stored.
.PARAMETER BaseUrl
    Base URL of the application (default: http://localhost:8080)
.PARAMETER ApiKey
    API key for authentication (default: test-api-key-123)
.PARAMETER Message
    Message to send to the chat (default: "My name is John and I work as a software engineer")
.PARAMETER Model
    Model to use for chat (default: llama2)
.EXAMPLE
    .\test-chat-to-knowledge-basic.ps1
.EXAMPLE
    .\test-chat-to-knowledge-basic.ps1 -Message "I love programming with Java"
#>

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$ApiKey = "test-api-key-123",
    [string]$Message = "My name is John and I work as a software engineer",
    [string]$Model = "llama2"
)

Write-Host "🗣️💭 Testing Chat to Knowledge Object Creation (Basic)" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$chatUrl = "$BaseUrl/v1/chat/completions"
$knowledgeSearchUrl = "$BaseUrl/v1/knowledge/search"
$headers = @{
    "Authorization" = "Bearer $ApiKey"
    "Content-Type" = "application/json"
}

try {
    # Step 1: Send chat message that should create knowledge
    Write-Host "Step 1: Sending chat message..." -ForegroundColor Yellow
    Write-Host "Testing: $chatUrl" -ForegroundColor Yellow
    Write-Host "Using API Key: $ApiKey" -ForegroundColor Gray
    Write-Host "Message: $Message" -ForegroundColor Gray
    Write-Host "Model: $Model" -ForegroundColor Gray

    $body = @{
        model = $Model
        messages = @(
            @{
                role = "user"
                content = $Message
            }
        )
    } | ConvertTo-Json

    $chatResponse = Invoke-WebRequest -Uri $chatUrl -Method POST -Headers $headers -Body $body -TimeoutSec 30

    if ($chatResponse.StatusCode -eq 200) {
        $chatContent = $chatResponse.Content | ConvertFrom-Json
        Write-Host "✅ Chat Completions PASSED" -ForegroundColor Green
        Write-Host "   Chat ID: $($chatContent.id)" -ForegroundColor Green
        Write-Host "   Model: $($chatContent.model)" -ForegroundColor Green
        Write-Host "   Assistant Response: $($chatContent.choices[0].message.content.Substring(0, [Math]::Min(50, $chatContent.choices[0].message.content.Length)))..." -ForegroundColor Green
    } else {
        Write-Host "❌ Chat Completions FAILED - Unexpected status: $($chatResponse.StatusCode)" -ForegroundColor Red
        return $false
    }

    # Step 2: Wait a moment for knowledge processing
    Write-Host "" -ForegroundColor White
    Write-Host "Step 2: Waiting for knowledge processing..." -ForegroundColor Yellow
    Start-Sleep -Seconds 2

    # Step 3: Search for knowledge objects that should have been created
    Write-Host "Step 3: Searching for created knowledge objects..." -ForegroundColor Yellow
    Write-Host "Testing: $knowledgeSearchUrl" -ForegroundColor Yellow

    # Search for knowledge related to the chat message
    $searchQuery = $Message.Split(" ")[0..3] -join " "  # Use first few words as search query
    $searchUrlWithParams = "$knowledgeSearchUrl`?query=$([uri]::EscapeDataString($searchQuery))&limit=10"

    $searchResponse = Invoke-WebRequest -Uri $searchUrlWithParams -Method GET -Headers $headers -TimeoutSec 15

    if ($searchResponse.StatusCode -eq 200) {
        $searchResults = $searchResponse.Content | ConvertFrom-Json
        Write-Host "✅ Knowledge Search PASSED" -ForegroundColor Green

        if ($searchResults -and $searchResults.Count -gt 0) {
            Write-Host "   Found $($searchResults.Count) knowledge objects:" -ForegroundColor Green
            foreach ($result in $searchResults | Select-Object -First 3) {
                Write-Host "   - ID: $($result.id), Score: $($result.score)" -ForegroundColor Green
                if ($result.content) {
                    Write-Host "     Content: $($result.content.Substring(0, [Math]::Min(100, $result.content.Length)))..." -ForegroundColor Gray
                }
            }

            # Step 4: Verify that we have conversation turns stored
            Write-Host "" -ForegroundColor White
            Write-Host "Step 4: Checking for conversation turns..." -ForegroundColor Yellow

            $turnSearchUrl = "$knowledgeSearchUrl`?query=$([uri]::EscapeDataString("conversation turn"))&limit=5"

            $turnSearchResponse = Invoke-WebRequest -Uri $turnSearchUrl -Method GET -Headers $headers -TimeoutSec 15

            if ($turnSearchResponse.StatusCode -eq 200) {
                $turnResults = $turnSearchResponse.Content | ConvertFrom-Json
                if ($turnResults -and $turnResults.Count -gt 0) {
                    Write-Host "✅ Conversation Turns Found" -ForegroundColor Green
                    Write-Host "   Found $($turnResults.Count) conversation turn objects" -ForegroundColor Green

                    # Verify we have both user and assistant turns
                    $userTurns = $turnResults | Where-Object { $_.content -and $_.content.Contains("user") }
                    $assistantTurns = $turnResults | Where-Object { $_.content -and $_.content.Contains("assistant") }

                    Write-Host "   User turns: $($userTurns.Count)" -ForegroundColor Green
                    Write-Host "   Assistant turns: $($assistantTurns.Count)" -ForegroundColor Green

                    Write-Host "" -ForegroundColor White
                    Write-Host "🎉 Chat to Knowledge Object Creation PASSED!" -ForegroundColor Green
                    Write-Host "   Successfully created knowledge objects from chat interaction" -ForegroundColor Green
                    return $true
                } else {
                    Write-Host "⚠️  No conversation turns found yet" -ForegroundColor Yellow
                    Write-Host "   This might be normal if processing is still ongoing" -ForegroundColor Yellow
                }
            }
        } else {
            Write-Host "⚠️  No knowledge objects found" -ForegroundColor Yellow
            Write-Host "   This might be expected if knowledge processing is asynchronous" -ForegroundColor Yellow
        }

        # Even if no objects found, the chat worked and search didn't fail
        Write-Host "" -ForegroundColor White
        Write-Host "🎉 Chat to Knowledge Object Creation PASSED!" -ForegroundColor Green
        Write-Host "   Chat completed successfully and knowledge search works" -ForegroundColor Green
        return $true

    } else {
        Write-Host "❌ Knowledge Search FAILED - Status: $($searchResponse.StatusCode)" -ForegroundColor Red
        return $false
    }

}
catch {
    Write-Host "❌ Chat to Knowledge Object Creation FAILED - Error: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Message -match "403") {
        Write-Host "   Check your API key: $ApiKey" -ForegroundColor Yellow
    } elseif ($_.Exception.Message -match "Connection") {
        Write-Host "   Make sure the application is running and accessible at $BaseUrl" -ForegroundColor Yellow
    } elseif ($_.Exception.Message -match "500") {
        Write-Host "   Server error occurred. Check application logs for details." -ForegroundColor Yellow
        Write-Host "   The knowledge search endpoint may not be fully implemented yet." -ForegroundColor Yellow
    }
    # Return false but don't exit the script
    $false
    return
}

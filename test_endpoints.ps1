# Test script for API endpoints
Write-Host "Testing API endpoints..."

# Test health endpoint
Write-Host "Testing /actuator/health..."
try {
    $response = [System.Net.WebRequest]::Create("http://localhost:8080/actuator/health")
    $response.Method = "GET"
    $response.Timeout = 5000
    $result = $response.GetResponse()
    Write-Host "Health endpoint: $($result.StatusCode)"
    $result.Close()
} catch {
    Write-Host "Health endpoint failed: $($_.Exception.Message)"
}

# Test models endpoint
Write-Host "Testing /v1/models..."
try {
    $response = [System.Net.WebRequest]::Create("http://localhost:8080/v1/models")
    $response.Method = "GET"
    $response.Timeout = 5000
    $result = $response.GetResponse()
    Write-Host "Models endpoint: $($result.StatusCode)"
    $result.Close()
} catch {
    Write-Host "Models endpoint failed: $($_.Exception.Message)"
}

# Test chat endpoint
Write-Host "Testing /v1/chat/completions..."
try {
    $response = [System.Net.WebRequest]::Create("http://localhost:8080/v1/chat/completions")
    $response.Method = "POST"
    $response.ContentType = "application/json"
    $response.Timeout = 5000
    $result = $response.GetResponse()
    Write-Host "Chat endpoint: $($result.StatusCode)"
    $result.Close()
} catch {
    Write-Host "Chat endpoint failed: $($_.Exception.Message)"
}

Write-Host "Testing complete."

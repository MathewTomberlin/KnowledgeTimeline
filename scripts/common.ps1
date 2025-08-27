# Common functions for Knowledge-Aware LLM Middleware scripts
# This file contains shared functions used by multiple scripts

# Set error action preference
$ErrorActionPreference = "Continue"



# Color constants for consistent output
$Colors = @{
    Success = "Green"
    Error = "Red"
    Warning = "Yellow"
    Info = "Cyan"
    Default = "White"
}

# Service configuration
$ServiceConfig = @{
    postgres = @{
        Name = "knowledge-postgres"
        Port = 5432
        HealthCheck = "pg_isready -U postgres -d knowledge_middleware"
        StartupTime = 30
    }
    redis = @{
        Name = "knowledge-redis"
        Port = 6379
        HealthCheck = "redis-cli ping"
        StartupTime = 10
    }
    ollama = @{
        Name = "knowledge-ollama"
        Port = 11434
        HealthCheck = "wget --no-verbose --tries=1 --spider http://localhost:11434/api/tags"
        StartupTime = 10
    }
    middleware = @{
        Name = "knowledge-middleware"
        Port = 8080
        HealthCheck = "curl -f http://localhost:8080/actuator/health"
        StartupTime = 60
    }
}

# Function to write colored output
function Write-ColorOutput {
    param(
        [string]$Message,
        [string]$Color = "White",
        [switch]$NoNewline
    )
    
    if ($NoNewline) {
        Write-Host $Message -ForegroundColor $Color -NoNewline
    } else {
        Write-Host $Message -ForegroundColor $Color
    }
}

# Function to check if Docker is running
function Test-DockerRunning {
    try {
        $null = docker version 2>$null
        return $true
    } catch {
        return $false
    }
}

# Function to check if required ports are available
function Test-PortsAvailable {
    $requiredPorts = @(5432, 6379, 8080, 11434)
    $conflicts = @()
    
    foreach ($port in $requiredPorts) {
        try {
            $connection = Test-NetConnection -ComputerName localhost -Port $port -InformationLevel Quiet -WarningAction SilentlyContinue 2>$null
            if ($connection.TcpTestSucceeded) {
                $conflicts += $port
            }
        } catch {
            # Port is available
        }
    }
    
    if ($conflicts.Count -gt 0) {
        Write-ColorOutput "Port conflicts detected:" $Colors.Warning
        foreach ($port in $conflicts) {
            Write-ColorOutput "   Port $port is already in use" $Colors.Warning
        }
        return $false
    }
    
    return $true
}

# Function to start a specific service
function Start-Service {
    param([string]$ServiceName)
    
    if (-not $ServiceConfig.ContainsKey($ServiceName)) {
        Write-ColorOutput "Unknown service: $ServiceName" $Colors.Error
        return $false
    }
    
    $config = $ServiceConfig[$ServiceName]
    
    try {
        # Start the service using docker-compose
        $result = docker-compose up -d $ServiceName 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            # Wait for service to be ready
            Start-Sleep -Seconds 5
            
            # Check if service is running
            $containerStatus = docker-compose ps $ServiceName --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" 2>$null
            
            if ($containerStatus -match "Up") {
                return $true
            } else {
                Write-ColorOutput "Service $ServiceName failed to start properly" $Colors.Error
                return $false
            }
        } else {
            Write-ColorOutput "Failed to start service $ServiceName" $Colors.Error
            Write-ColorOutput "   Error: $result" $Colors.Error
            return $false
        }
    } catch {
        Write-ColorOutput "Exception starting service $($ServiceName): $($_.Exception.Message)" $Colors.Error
        return $false
    }
}

# Function to stop a specific service
function Stop-Service {
    param([string]$ServiceName)
    
    if (-not $ServiceConfig.ContainsKey($ServiceName)) {
        Write-ColorOutput "Unknown service: $ServiceName" $Colors.Error
        return $false
    }
    
    try {
        # Stop the service using docker-compose
        $result = docker-compose stop $ServiceName 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-ColorOutput "Service $ServiceName stopped successfully" $Colors.Success
            return $true
        } else {
            Write-ColorOutput "Failed to stop service $ServiceName" $Colors.Error
            Write-ColorOutput "   Error: $result" $Colors.Error
            return $false
        }
    } catch {
        Write-ColorOutput "Exception stopping service $($ServiceName): $($_.Exception.Message)" $Colors.Error
        return $false
    }
}

# Function to wait for services to be healthy
function Wait-ForServicesHealthy {
    param([string[]]$Services)
    
    $maxWaitTime = 300 # 5 minutes
    $startTime = Get-Date
    $healthyServices = @()
    
    Write-ColorOutput "Waiting for services to be healthy..." $Colors.Info
    
    while ($healthyServices.Count -lt $Services.Count) {
        $elapsed = (Get-Date) - $startTime
        
        if ($elapsed.TotalSeconds -gt $maxWaitTime) {
            Write-ColorOutput "Timeout waiting for services to be healthy" $Colors.Error
            return $false
        }
        
        # Show current status for all services
        Write-ColorOutput "`nCurrent service status:" $Colors.Info
        foreach ($service in $Services) {
            if ($healthyServices -contains $service) {
                Write-ColorOutput "   $($service): Healthy" $Colors.Success
            } else {
                $status = "Checking..."
                try {
                    $containerStatus = docker-compose ps $service --format "{{.Status}}" 2>$null
                    if ($containerStatus -match "Up") {
                        $status = "Running (checking health)"
                    } elseif ($containerStatus -match "Starting") {
                        $status = "Starting up"
                    } elseif ($containerStatus -match "Exit") {
                        $status = "Exited"
                    } else {
                        $status = "Unknown"
                    }
                } catch {
                    $status = "Error checking status"
                }
                Write-ColorOutput "   $service`: $status" $Colors.Warning
            }
        }
        
        foreach ($service in $Services) {
            if ($healthyServices -contains $service) {
                continue
            }
            
            if (Test-ServiceHealthy $service) {
                $healthyServices += $service
                Write-ColorOutput "`n   $service is now healthy!" $Colors.Success
            }
        }
        
        if ($healthyServices.Count -lt $Services.Count) {
            $remaining = $Services.Count - $healthyServices.Count
            $elapsedFormatted = [math]::Round($elapsed.TotalSeconds, 0)
            Write-ColorOutput "`n   Waiting for $remaining more service(s)... (elapsed: ${elapsedFormatted}s)" $Colors.Info
            Start-Sleep -Seconds 5
        }
    }
    
    return $true
}

# Function to test if a service is healthy
function Test-ServiceHealthy {
    param([string]$ServiceName)
    
    if (-not $ServiceConfig.ContainsKey($ServiceName)) {
        return $false
    }
    
    $config = $ServiceConfig[$ServiceName]
    
    try {
        # Check if container is running and healthy using docker-compose
        $containerStatus = docker-compose ps $ServiceName --format "{{.Status}}" 2>$null
        
        if (-not ($containerStatus -match "Up")) {
            return $false
        }
        
        # Check if container is healthy (look for "healthy" in status)
        if ($containerStatus -match "healthy") {
            return $true
        }
        
        # For middleware, check HTTP health endpoint
        if ($ServiceName -eq "middleware") {
            try {
                $response = Invoke-WebRequest -Uri "http://localhost:$($config.Port)/actuator/health" -UseBasicParsing -TimeoutSec 10 2>$null
                return $response.StatusCode -eq 200
            } catch {
                return $false
            }
        }
        
        # For Ollama, check the actual API endpoint since port connectivity isn't sufficient
        if ($ServiceName -eq "ollama") {
            try {
                $response = Invoke-WebRequest -Uri "http://localhost:$($config.Port)/api/tags" -UseBasicParsing -TimeoutSec 5 2>$null
                return $response.StatusCode -eq 200
            } catch {
                return $false
            }
        }

        # For other services, check if they're responding on their port
        try {
            $connection = Test-NetConnection -ComputerName localhost -Port $config.Port -InformationLevel Quiet -WarningAction SilentlyContinue 2>$null
            return $connection.TcpTestSucceeded
        } catch {
            return $false
        }
        
    } catch {
        return $false
    }
}

# Function to get service status
function Get-ServiceStatus {
    param([string[]]$Services = @("postgres", "redis", "ollama", "middleware"))
    
    Write-ColorOutput "Service Status:" "Cyan"
    Write-ColorOutput "==============" "Cyan"
    
    foreach ($service in $Services) {
        if (-not $ServiceConfig.ContainsKey($service)) {
            continue
        }
        
        $config = $ServiceConfig[$service]
        $status = "Unknown"
        $color = $Colors.Default
        
        try {
            $containerStatus = docker-compose ps $service --format "{{.Status}}" 2>$null
            
            if ($containerStatus -match "Up") {
                if (Test-ServiceHealthy $service) {
                    $status = "Healthy"
                    $color = "Green"
                } else {
                    $status = "Running (Unhealthy)"
                    $color = "Yellow"
                }
            } elseif ($containerStatus -match "Exit") {
                $status = "Exited"
                $color = "Red"
            } else {
                $status = "Stopped"
                $color = "Red"
            }
        } catch {
            $status = "Error"
            $color = "Red"
        }
        
        Write-ColorOutput "   $($service): $status" $color
    }
}

# Function to build the application
function Invoke-BuildApplication {
    try {
        Write-ColorOutput "   Running Maven build..." $Colors.Info
        
        # Check if Maven wrapper exists
        if (Test-Path "mvnw.cmd") {
            $result = & ".\mvnw.cmd" clean compile 2>&1
        } elseif (Test-Path "mvnw") {
            $result = & ".\mvnw" clean compile 2>&1
        } else {
            $result = & mvn clean compile 2>&1
        }
        
        if ($LASTEXITCODE -eq 0) {
            return $true
        } else {
            Write-ColorOutput "   Build output: $result" $Colors.Error
            return $false
        }
    } catch {
        Write-ColorOutput "   Build exception: $($_.Exception.Message)" $Colors.Error
        return $false
    }
}

# Function to check if a container exists
function Test-ContainerExists {
    param([string]$ContainerName)
    
    try {
        $null = docker ps -a --filter "name=$ContainerName" --format "{{.Names}}"
        return $LASTEXITCODE -eq 0
    } catch {
        return $false
    }
}

# Functions are available for use in other scripts when dot-sourced

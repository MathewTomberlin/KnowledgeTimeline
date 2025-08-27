# KnowledgeTimeline - API Test Scripts

This directory contains comprehensive test scripts for validating the KnowledgeTimeline application functionality.

## 📋 Test Scripts Overview

| Script | Description | PowerShell | Batch File |
|--------|-------------|------------|------------|
| **test-health.ps1** | Test application health endpoint | ✅ | ✅ |
| **test-models.ps1** | Test available models endpoint | ✅ | ✅ |
| **test-chat.ps1** | Test chat completions functionality | ✅ | ✅ |
| **test-knowledge-create.ps1** | Test knowledge object creation | ✅ | ✅ |
| **test-embeddings.ps1** | Test embedding generation | ✅ | ✅ |
| **test-chat-to-knowledge-basic.ps1** | Test basic chat to knowledge object creation | ✅ | ✅ |
| **test-chat-to-knowledge-memory-extraction.ps1** | Test memory extraction from chat conversations | ✅ | ✅ |
| **test-embeddings-clean.ps1** | Test embedding generation (clean) | ✅ | ✅ |
| **test-knowledge-create-clean.ps1** | Test knowledge object creation (clean) | ✅ | ✅ |
| **run-all-tests.ps1** | Run comprehensive test suite | ✅ | ✅ |
| **run-all-tests-simple.ps1** | Run simple test suite | ✅ | ✅ |

## 🚀 Quick Start

### Running All Tests
```bash
# PowerShell
.\scripts\tests\run-all-tests.ps1

# Batch file
.\scripts\tests\run-all-tests.bat
```

### Running Individual Tests
```bash
# Health Check
.\scripts\tests\test-health.ps1
.\scripts\tests\test-health.bat

# Models API
.\scripts\tests\test-models.ps1
.\scripts\tests\test-models.bat

# Chat Completions
.\scripts\tests\test-chat.ps1
.\scripts\tests\test-chat.bat

# Knowledge Creation
.\scripts\tests\test-knowledge-create.ps1
.\scripts\tests\test-knowledge-create.bat

# Embeddings
.\scripts\tests\test-embeddings.ps1
.\scripts\tests\test-embeddings.bat
```

## ⚙️ Configuration

### Default Parameters
- **BaseUrl**: `http://localhost:8080`
- **ApiKey**: `test-api-key-123`

### Custom Parameters
```powershell
# Custom base URL and API key
.\test-health.ps1 -BaseUrl "http://localhost:9090" -ApiKey "your-api-key"

# Stop on first failure
.\run-all-tests.ps1 -StopOnFailure
```

## 📝 Test Details

### 1. Health Check (`test-health.ps1`)
- **Endpoint**: `GET /actuator/health`
- **Purpose**: Verify application is running and healthy
- **Expected**: HTTP 200 with `{"status":"UP"}`
- **Authentication**: Not required

```powershell
# Basic usage
.\test-health.ps1

# Custom URL
.\test-health.ps1 -BaseUrl "http://localhost:9090"
```

### 2. Models API (`test-models.ps1`)
- **Endpoint**: `GET /v1/models`
- **Purpose**: Verify available LLM models
- **Expected**: HTTP 200 with list of models
- **Authentication**: Bearer token required

```powershell
# Basic usage
.\test-models.ps1

# Custom parameters
.\test-models.ps1 -ApiKey "custom-key" -BaseUrl "http://localhost:9090"
```

### 3. Chat Completions (`test-chat.ps1`)
- **Endpoint**: `POST /v1/chat/completions`
- **Purpose**: Test chat functionality with LLM
- **Expected**: HTTP 200 with OpenAI-compatible response
- **Authentication**: Bearer token required

```powershell
# Basic usage
.\test-chat.ps1

# Custom message and model
.\test-chat.ps1 -Message "What is AI?" -Model "llama2" -ApiKey "custom-key"
```

### 4. Knowledge Creation (`test-knowledge-create.ps1`)
- **Endpoint**: `POST /v1/knowledge/objects`
- **Purpose**: Test knowledge object creation
- **Expected**: HTTP 200 with created object details
- **Authentication**: Bearer token required

```powershell
# Basic usage
.\test-knowledge-create.ps1

# Custom content
.\test-knowledge-create.ps1 -Title "My Doc" -Content "Custom content" -Type "TURN"
```

### 5. Embeddings (`test-embeddings.ps1`)
- **Endpoint**: `POST /v1/embeddings`
- **Purpose**: Test embedding generation
- **Expected**: HTTP 200 with embedding vectors
- **Authentication**: Bearer token required

```powershell
# Basic usage
.\test-embeddings.ps1

# Custom text
.\test-embeddings.ps1 -Text "This is a test sentence for embeddings"
```

### 6. Chat to Knowledge Object Creation (Basic) (`test-chat-to-knowledge-basic.ps1`)
- **Endpoints**: `POST /v1/chat/completions`, `GET /v1/knowledge/search`
- **Purpose**: Test integration between chat completions and knowledge object creation
- **Expected**: HTTP 200 responses, knowledge objects created from chat interactions
- **Authentication**: Bearer token required

```powershell
# Basic usage
.\test-chat-to-knowledge-basic.ps1

# Custom message
.\test-chat-to-knowledge-basic.ps1 -Message "I am a project manager at Google"
```

### 7. Memory Extraction from Chat (`test-chat-to-knowledge-memory-extraction.ps1`)
- **Endpoints**: `POST /v1/chat/completions`, `GET /v1/knowledge/search`
- **Purpose**: Test memory extraction and fact creation from chat conversations
- **Expected**: HTTP 200 responses, extracted facts stored as knowledge objects
- **Authentication**: Bearer token required

```powershell
# Basic usage
.\test-chat-to-knowledge-memory-extraction.ps1

# Custom message with extractable facts
.\test-chat-to-knowledge-memory-extraction.ps1 -Message "My name is Alex, I'm 30, and I work as a data scientist in New York"
```

### 8. Comprehensive Test Suite (`run-all-tests.ps1`)
- **Purpose**: Run all tests with summary report
- **Features**:
  - Sequential test execution
  - Detailed results summary
  - Timing information
  - Stop-on-failure option

```powershell
# Basic usage
.\run-all-tests.ps1

# Stop on first failure
.\run-all-tests.ps1 -StopOnFailure

# Custom configuration
.\run-all-tests.ps1 -BaseUrl "http://localhost:9090" -ApiKey "custom-key" -StopOnFailure
```

## 📊 Test Results

### Success Indicators
- ✅ **PASSED**: Test completed successfully
- ❌ **FAILED**: Test failed (non-critical error)
- 💥 **ERROR**: Test encountered an exception

### Sample Output
```
🚀 KnowledgeTimeline - Comprehensive Test Suite
==================================================
Base URL: http://localhost:8080
API Key: test-api-key-123
Stop on Failure: False

📋 Running Test: Health Check
   Description: Verify application health
   Script: test-health.ps1

🩺 Testing Health Endpoint
=====================================
Testing: http://localhost:8080/actuator/health
✅ Health Check PASSED
   Status: UP
   Response Time: N/A

✅ Health Check PASSED (in 0.45s)
────────────────────────────────────────────────────────

📋 Running Test: Models API
   Description: Test available models
   Script: test-models.ps1

🤖 Testing Models Endpoint
=================================
Testing: http://localhost:8080/v1/models
Using API Key: test-api-key-123
✅ Models Endpoint PASSED
   Found 1 model(s):
   - llama2 (owned by ollama)

✅ Models API PASSED (in 0.78s)

[... more tests ...]

📊 Test Summary
===============
Total Tests: 5
✅ Passed: 5
❌ Failed: 0
💥 Errors: 0
⏱️  Total Time: 3.45s

📋 Detailed Results:
✅ Health Check - 0.45s
✅ Models API - 0.78s
✅ Chat Completions - 1.23s
✅ Knowledge Creation - 0.67s
✅ Embeddings - 1.45s

🎉 ALL TESTS PASSED! 🎉
   The KnowledgeTimeline application is working correctly.
```

## 🔧 Prerequisites

### Application Setup
1. **Start the application services**:
   ```bash
   .\scripts\start-app.ps1
   ```

2. **Verify services are running**:
   ```bash
   .\scripts\status.ps1
   ```

### System Requirements
- **PowerShell 5.1+** (or PowerShell 7+ for best compatibility)
- **Windows Command Prompt** (for .bat files)
- **Running KnowledgeTimeline application**

## 🐛 Troubleshooting

### Common Issues

#### 403 Forbidden Error
- **Cause**: Invalid or missing API key
- **Solution**: Check your API key configuration
```powershell
.\test-models.ps1 -ApiKey "your-valid-api-key"
```

#### Connection Refused
- **Cause**: Application not running or wrong URL
- **Solution**: Verify application status and URL
```bash
.\scripts\status.ps1
.\test-health.ps1 -BaseUrl "http://localhost:8080"
```

#### JSON Parsing Errors
- **Cause**: Invalid response format or network issues
- **Solution**: Check application logs and network connectivity

### PowerShell Compatibility
- **Issue**: String terminator errors in older PowerShell versions
- **Solution**: Use PowerShell 7+ or update scripts for compatibility

## 📁 File Structure

```
scripts/tests/
├── README.md                    # This documentation
├── test-health.ps1             # Health check test
├── test-health.bat             # Batch wrapper for health test
├── test-models.ps1             # Models API test
├── test-models.bat             # Batch wrapper for models test
├── test-chat.ps1               # Chat completions test
├── test-chat.bat               # Batch wrapper for chat test
├── test-knowledge-create.ps1   # Knowledge creation test
├── test-knowledge-create.bat   # Batch wrapper for knowledge test
├── test-embeddings.ps1         # Embeddings test
├── test-embeddings.bat         # Batch wrapper for embeddings test
├── run-all-tests.ps1           # Comprehensive test suite
└── run-all-tests.bat           # Batch wrapper for full suite
```

## 🎯 Best Practices

### Test Execution
1. **Start with health check**: Verify application is running
2. **Test authentication**: Ensure API keys work
3. **Run comprehensive suite**: Use `run-all-tests.ps1` for full validation
4. **Check logs**: Monitor application logs during testing

### Custom Testing
- **Modify parameters**: Adjust URLs, API keys, and test data
- **Add new tests**: Follow the existing pattern for new endpoints
- **Error handling**: Include proper error messages and troubleshooting tips

## 📞 Support

For issues with the test scripts:
1. Check the troubleshooting section above
2. Review application logs: `.\scripts\logs.ps1 -Service middleware`
3. Verify application status: `.\scripts\status.ps1`
4. Ensure all prerequisites are met

---

**Note**: These test scripts are designed for local development and testing. Modify parameters as needed for your specific environment.

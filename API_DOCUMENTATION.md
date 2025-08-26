# API Documentation - Knowledge-Aware LLM Middleware

## Overview
This document provides comprehensive API documentation for the Knowledge-Aware LLM Middleware. The API follows OpenAI-compatible patterns while adding knowledge-aware capabilities.

## Authentication
All API endpoints require authentication using API keys.

### Header Format
```
Authorization: Bearer <api-key>
```

### API Key Management
- API keys are tenant-specific
- Keys can be created, rotated, and revoked
- Rate limits are applied per API key

## Base URL
- **Local Development**: `http://localhost:8080`
- **Docker Environment**: `http://localhost:8080`
- **Production**: `https://api.knowledgetimeline.com`

## OpenAI-Compatible Endpoints

### 1. Chat Completions
**Endpoint**: `POST /v1/chat/completions`

**Description**: Generate chat completions with knowledge-aware context.

**Request Body**:
```json
{
  "model": "gpt-4",
  "messages": [
    {
      "role": "user",
      "content": "What do you know about machine learning?"
    }
  ],
  "temperature": 0.7,
  "max_tokens": 1000,
  "knowledge_context": {
    "include_recent": true,
    "include_related": true,
    "max_context_objects": 10,
    "similarity_threshold": 0.8
  }
}
```

**Response**:
```json
{
  "id": "chatcmpl-123",
  "object": "chat.completion",
  "created": 1677652288,
  "model": "gpt-4",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "Based on our knowledge base..."
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 100,
    "completion_tokens": 200,
    "total_tokens": 300
  },
  "knowledge_context": {
    "objects_used": [
      {
        "id": "obj-123",
        "type": "DOCUMENT",
        "title": "Machine Learning Basics",
        "relevance_score": 0.95
      }
    ],
    "relationships_found": 3
  }
}
```

### 2. Embeddings
**Endpoint**: `POST /v1/embeddings`

**Description**: Generate embeddings for text content.

**Request Body**:
```json
{
  "model": "text-embedding-ada-002",
  "input": "Machine learning is a subset of artificial intelligence.",
  "store_knowledge": true,
  "metadata": {
    "source": "user_input",
    "category": "technology"
  }
}
```

**Response**:
```json
{
  "object": "list",
  "data": [
    {
      "object": "embedding",
      "embedding": [0.1, 0.2, 0.3, ...],
      "index": 0
    }
  ],
  "model": "text-embedding-ada-002",
  "usage": {
    "prompt_tokens": 10,
    "total_tokens": 10
  },
  "knowledge_object_id": "obj-456"
}
```

### 3. Models
**Endpoint**: `GET /v1/models`

**Description**: List available models and their capabilities.

**Response**:
```json
{
  "object": "list",
  "data": [
    {
      "id": "gpt-4",
      "object": "model",
      "created": 1677610602,
      "owned_by": "openai",
      "permission": [],
      "root": "gpt-4",
      "parent": null,
      "knowledge_aware": true,
      "max_tokens": 8192
    },
    {
      "id": "text-embedding-ada-002",
      "object": "model",
      "created": 1671217299,
      "owned_by": "openai",
      "permission": [],
      "root": "text-embedding-ada-002",
      "parent": null,
      "knowledge_aware": false,
      "max_tokens": 8191
    }
  ]
}
```

## Knowledge Management Endpoints

### 4. Knowledge Objects
**Endpoint**: `GET /api/objects`

**Description**: Retrieve knowledge objects with filtering and pagination.

**Query Parameters**:
- `type` - Filter by object type (DOCUMENT, CONVERSATION, FACT, etc.)
- `query` - Search in content and metadata
- `page` - Page number (default: 0)
- `size` - Page size (default: 20)
- `sort` - Sort field (default: createdAt)
- `order` - Sort order (asc/desc, default: desc)

**Response**:
```json
{
  "content": [
    {
      "id": "obj-123",
      "type": "DOCUMENT",
      "title": "Machine Learning Basics",
      "content": "Machine learning is...",
      "metadata": {
        "source": "upload",
        "category": "technology"
      },
      "createdAt": "2024-01-01T00:00:00Z",
      "updatedAt": "2024-01-01T00:00:00Z",
      "archived": false
    }
  ],
  "totalElements": 100,
  "totalPages": 5,
  "size": 20,
  "number": 0
}
```

**Endpoint**: `POST /api/objects`

**Description**: Create a new knowledge object.

**Request Body**:
```json
{
  "type": "DOCUMENT",
  "title": "New Document",
  "content": "Document content...",
  "metadata": {
    "source": "manual",
    "category": "general"
  },
  "variants": [
    {
      "type": "SUMMARY",
      "content": "Document summary..."
    }
  ]
}
```

**Endpoint**: `PUT /api/objects/{id}`

**Description**: Update an existing knowledge object.

**Endpoint**: `DELETE /api/objects/{id}`

**Description**: Archive a knowledge object (soft delete).

### 5. Knowledge Relationships
**Endpoint**: `GET /api/relationships`

**Description**: Retrieve relationships between knowledge objects.

**Query Parameters**:
- `source_id` - Filter by source object ID
- `target_id` - Filter by target object ID
- `type` - Filter by relationship type
- `page` - Page number
- `size` - Page size

**Response**:
```json
{
  "content": [
    {
      "id": "rel-123",
      "sourceId": "obj-123",
      "targetId": "obj-456",
      "type": "RELATES_TO",
      "strength": 0.85,
      "metadata": {
        "reason": "Similar topic"
      },
      "createdAt": "2024-01-01T00:00:00Z"
    }
  ],
  "totalElements": 50,
  "totalPages": 3,
  "size": 20,
  "number": 0
}
```

**Endpoint**: `POST /api/relationships`

**Description**: Create a new relationship between objects.

**Request Body**:
```json
{
  "sourceId": "obj-123",
  "targetId": "obj-456",
  "type": "RELATES_TO",
  "strength": 0.85,
  "metadata": {
    "reason": "Similar topic"
  }
}
```

### 6. Similarity Search
**Endpoint**: `POST /api/search/similar`

**Description**: Find similar knowledge objects using vector similarity.

**Request Body**:
```json
{
  "query": "machine learning algorithms",
  "max_results": 10,
  "similarity_threshold": 0.7,
  "object_types": ["DOCUMENT", "FACT"],
  "include_relationships": true
}
```

**Response**:
```json
{
  "results": [
    {
      "object": {
        "id": "obj-123",
        "type": "DOCUMENT",
        "title": "Machine Learning Basics",
        "content": "..."
      },
      "similarity_score": 0.95,
      "relationships": [
        {
          "id": "rel-123",
          "type": "RELATES_TO",
          "targetId": "obj-456"
        }
      ]
    }
  ],
  "total_found": 25
}
```

## Background Job Endpoints

### 7. Session Summarization
**Endpoint**: `POST /jobs/session-summarize`

**Description**: Trigger background summarization of dialogue sessions.

**Query Parameters**:
- `tenant_id` - Tenant ID (optional, uses authenticated tenant if not provided)
- `session_id` - Specific session ID to summarize (optional)
- `user_id` - User ID to scope sessions (optional)
- `batch_size` - Maximum sessions to process (default: 100)

**Response**:
```json
{
  "status": "success",
  "message": "Session summarization job initiated.",
  "sessionsSummarized": 5
}
```

### 8. Job Health
**Endpoint**: `GET /jobs/health`

**Description**: Check health status of background job services.

**Response**:
```json
{
  "status": "healthy",
  "endpoints": {
    "session-summarize": "POST /jobs/session-summarize"
  }
}
```

## Management Endpoints

### 9. Tenant Management
**Endpoint**: `GET /api/tenants`

**Description**: Get current tenant information.

**Response**:
```json
{
  "id": "tenant-123",
  "name": "Acme Corp",
  "plan": "PROFESSIONAL",
  "createdAt": "2024-01-01T00:00:00Z",
  "limits": {
    "max_objects": 10000,
    "max_relationships": 50000,
    "rate_limit_per_minute": 1000
  },
  "usage": {
    "objects_count": 1500,
    "relationships_count": 7500,
    "tokens_used_this_month": 500000
  }
}
```

### 10. Usage Statistics
**Endpoint**: `GET /api/usage`

**Description**: Get usage statistics for the current tenant.

**Query Parameters**:
- `start_date` - Start date (ISO format)
- `end_date` - End date (ISO format)
- `granularity` - Data granularity (hour, day, month)

**Response**:
```json
{
  "period": {
    "start": "2024-01-01T00:00:00Z",
    "end": "2024-01-31T23:59:59Z"
  },
  "metrics": {
    "total_requests": 15000,
    "total_tokens": 2500000,
    "total_cost": 125.50,
    "average_response_time": 250
  },
  "by_model": {
    "gpt-4": {
      "requests": 8000,
      "tokens": 1500000,
      "cost": 75.00
    },
    "text-embedding-ada-002": {
      "requests": 7000,
      "tokens": 1000000,
      "cost": 50.50
    }
  }
}
```

## Health and Monitoring

### 11. Application Health
**Endpoint**: `GET /actuator/health`

**Description**: Check application health status.

**Response**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "version": "7.0.0"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 1000000000000,
        "free": 500000000000,
        "threshold": 10485760
      }
    }
  }
}
```

### 12. Application Info
**Endpoint**: `GET /actuator/info`

**Description**: Get application information.

**Response**:
```json
{
  "app": {
    "name": "Knowledge-Aware LLM Middleware",
    "version": "1.0.0",
    "description": "Middleware for knowledge-aware LLM applications"
  },
  "git": {
    "commit": {
      "id": "abc123",
      "time": "2024-01-01T00:00:00Z"
    },
    "branch": "main"
  }
}
```

## Error Handling

### Error Response Format
All endpoints return errors in a consistent format:

```json
{
  "status": "error",
  "error": {
    "code": "INVALID_REQUEST",
    "message": "Request validation failed",
    "details": {
      "field": "model",
      "reason": "Model 'invalid-model' is not supported"
    }
  },
  "timestamp": "2024-01-01T00:00:00Z"
}
```

### Common Error Codes
- `INVALID_REQUEST` - Request validation failed
- `UNAUTHORIZED` - Authentication required
- `FORBIDDEN` - Insufficient permissions
- `NOT_FOUND` - Resource not found
- `RATE_LIMIT_EXCEEDED` - Rate limit exceeded
- `INTERNAL_ERROR` - Internal server error
- `SERVICE_UNAVAILABLE` - External service unavailable

## Rate Limiting

### Limits
- **Free Plan**: 100 requests/minute
- **Professional Plan**: 1000 requests/minute
- **Enterprise Plan**: 10000 requests/minute

### Headers
Rate limit information is included in response headers:
```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 950
X-RateLimit-Reset: 1640995200
```

## Pagination

### Standard Pagination
All list endpoints support pagination with the following parameters:
- `page` - Page number (0-based, default: 0)
- `size` - Page size (default: 20, max: 100)
- `sort` - Sort field
- `order` - Sort order (asc/desc)

### Pagination Response
```json
{
  "content": [...],
  "totalElements": 1000,
  "totalPages": 50,
  "size": 20,
  "number": 0,
  "first": true,
  "last": false,
  "numberOfElements": 20
}
```

## Webhooks

### Webhook Configuration
Webhooks can be configured to receive notifications for:
- Knowledge object creation/updates
- Relationship discoveries
- Usage threshold alerts
- System health events

### Webhook Payload
```json
{
  "event": "knowledge_object.created",
  "timestamp": "2024-01-01T00:00:00Z",
  "data": {
    "object_id": "obj-123",
    "type": "DOCUMENT",
    "title": "New Document"
  }
}
```

## SDKs and Libraries

### Official SDKs
- **Python**: `knowledge-middleware-python`
- **JavaScript**: `@knowledgetimeline/middleware`
- **Java**: `knowledge-middleware-java`

### Example Usage (Python)
```python
from knowledge_middleware import KnowledgeMiddleware

client = KnowledgeMiddleware(api_key="your-api-key")

# Chat completion with knowledge context
response = client.chat.completions.create(
    model="gpt-4",
    messages=[{"role": "user", "content": "What do you know about AI?"}],
    knowledge_context={
        "include_recent": True,
        "include_related": True
    }
)

# Create knowledge object
object = client.objects.create(
    type="DOCUMENT",
    title="AI Basics",
    content="Artificial intelligence is..."
)
```

## Versioning

### API Versioning
- Current version: v1
- Version specified in URL path: `/v1/`
- Backward compatibility maintained within major versions
- Deprecation notices provided 6 months in advance

### Changelog
- **v1.0.0** - Initial release with core functionality
- **v1.1.0** - Added relationship discovery
- **v1.2.0** - Enhanced similarity search

## Support

### Documentation
- **API Reference**: This document
- **Integration Guide**: `/docs/integration`
- **Examples**: `/docs/examples`

### Support Channels
- **Email**: support@knowledgetimeline.com
- **Discord**: KnowledgeTimeline Community
- **GitHub Issues**: For bug reports and feature requests

### Status Page
- **Status**: https://status.knowledgetimeline.com
- **Uptime**: Real-time system status
- **Incidents**: Current and historical incidents

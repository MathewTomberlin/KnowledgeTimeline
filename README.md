# Knowledge-Aware LLM Middleware

A Spring Boot-based middleware that provides OpenAI-compatible API endpoints with advanced knowledge management capabilities, including vector similarity search, context building, memory extraction, and multi-tenant architecture.

## 🚀 Features

### Core Functionality
- **OpenAI-Compatible API**: Drop-in replacement for OpenAI API with additional knowledge features
- **Multi-Tenant Architecture**: Isolated data and configuration per tenant
- **Vector Similarity Search**: Semantic search using embeddings (PostgreSQL pgvector)
- **Context Building**: Intelligent context assembly with token budget management
- **Memory Extraction**: Structured fact, entity, and task extraction from conversations
- **Session Management**: Dialogue state tracking and summarization
- **Usage Tracking**: Comprehensive token and cost tracking

### Technical Features
- **Spring Boot 3.2.0**: Modern Java framework with reactive support
- **PostgreSQL with pgvector**: Vector database for similarity search
- **Redis**: Caching and rate limiting
- **Flyway**: Database migration management
- **Docker Compose**: Complete local development environment
- **H2 Database**: In-memory database for testing
- **Spring Security**: API key authentication
- **Rate Limiting**: Configurable per-tenant rate limits
- **Caching**: Multi-level caching (Redis + Caffeine)

## 📋 Prerequisites

- Java 17 or higher
- Docker and Docker Compose
- Maven 3.6+
- PostgreSQL 15+ (for production)
- Redis 7+ (for production)

## 🛠️ Quick Start

### 1. Clone and Setup

```bash
git clone <repository-url>
cd KnowledgeTimeline
```

### 2. Start Local Development Environment

```bash
# Option 1: Use convenience scripts (recommended)
.\scripts\start-app.bat

# Option 2: Manual Docker Compose
docker-compose up -d

# Verify services are running
.\scripts\status.bat
```

### 3. Run the Application

```bash
# Using Maven
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Or using Docker
docker-compose up middleware
```

### 4. Test the API

```bash
# Health check
curl http://localhost:8080/actuator/health

# List models (requires API key)
curl -H "Authorization: Bearer your-api-key" http://localhost:8080/v1/models

# Chat completion
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Authorization: Bearer your-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-3.5-turbo",
    "messages": [{"role": "user", "content": "Hello!"}]
  }'

# Or use the test script
.\scripts\test-api.ps1
```

## 🛠️ Development Scripts

The project includes comprehensive PowerShell and batch scripts for easy development and management:

### Core Scripts
- **`start-app.bat`** / **`start-app.ps1`** - Start all services with proper dependency management
- **`stop-app.bat`** / **`stop-app.ps1`** - Stop all services gracefully  
- **`status.bat`** / **`status.ps1`** - Display current status of all services
- **`logs.bat`** / **`logs.ps1`** - View logs for specific services
- **`db-reset.bat`** / **`db-reset.ps1`** - Reset database and run fresh migrations

### Usage Examples
```bash
# Start application
.\scripts\start-app.bat

# Check status
.\scripts\status.bat

# View logs
.\scripts\logs.bat -Service middleware

# Reset database
.\scripts\db-reset.bat -Confirm
```

For detailed script documentation, see [scripts/README.md](scripts/README.md).

## 🏗️ Architecture

### Core Components

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   API Layer     │    │  Service Layer  │    │  Data Layer     │
│                 │    │                 │    │                 │
│ • ChatController│    │ • ChatService   │    │ • PostgreSQL    │
│ • ModelsController│  │ • ContextBuilder│    │ • Redis         │
│ • EmbeddingsController│ • MemoryExtraction│  │ • Vector Store  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────┐
                    │  External APIs  │
                    │                 │
                    │ • OpenAI        │
                    │ • Embeddings    │
                    │ • Vector Store  │
                    └─────────────────┘
```

### Data Model

- **Tenants**: Multi-tenant isolation
- **API Keys**: Authentication and authorization
- **Knowledge Objects**: Core knowledge entities (TURN, FILE_CHUNK, SUMMARY, etc.)
- **Content Variants**: Different representations of content (RAW, SHORT, MEDIUM, BULLET_FACTS)
- **Knowledge Relationships**: Connections between objects (SUPPORTS, REFERENCES, CONTRADICTS)
- **Dialogue States**: Session-level conversation tracking
- **Usage Logs**: Token and cost tracking

## 🔧 Configuration

### Environment Profiles

- **local**: Development with local services
- **docker**: Docker Compose environment
- **test**: H2 in-memory database for testing
- **production**: Production configuration

### Key Configuration Properties

```yaml
knowledge:
  token-budget:
    default: 2000
  retrieval:
    k: 40
    mmr-diversity: 0.3
    recency-decay: 0.03
  micro-quote:
    max-tokens: 120
  session:
    summarize-turns: 10
    summarize-tokens: 3000
  rate-limits:
    default-requests-per-minute: 60
    default-burst: 120
```

## 🧪 Testing

### Run Tests

```bash
# All tests
./mvnw test

# Specific test class
./mvnw test -Dtest=KnowledgeMiddlewareApplicationTests

# Integration tests only
./mvnw test -Dtest=*IntegrationTest
```

### Test Coverage

- ✅ Spring Boot context loading
- ✅ Database migrations (H2)
- ✅ Basic entity model validation
- 🔄 Service layer tests (in progress)
- 🔄 Integration tests (in progress)
- 🔄 API endpoint tests (in progress)

## 📊 Database Schema

### Core Tables

1. **tenants**: Tenant information and plans
2. **api_keys**: API key authentication
3. **knowledge_objects**: Core knowledge entities
4. **content_variants**: Content representations
5. **knowledge_relationships**: Object relationships
6. **dialogue_states**: Session management
7. **usage_logs**: Usage tracking
8. **embeddings**: Vector storage

### Migration Strategy

- **PostgreSQL**: Production schema with pgvector
- **H2**: Test schema with simplified vector storage
- **Flyway**: Automated migration management

## 🚀 Deployment

### Docker Deployment

```bash
# Build and run with Docker Compose
docker-compose up -d

# Or build standalone image
docker build -t knowledge-middleware .
docker run -p 8080:8080 knowledge-middleware
```

### Production Considerations

- Use PostgreSQL with pgvector extension
- Configure Redis for caching and rate limiting
- Set up proper API key management
- Configure monitoring and logging
- Use HTTPS in production
- Set up proper backup strategies

## 🔌 API Endpoints

### OpenAI-Compatible Endpoints

- `GET /v1/models` - List available models
- `POST /v1/chat/completions` - Chat completions
- `POST /v1/embeddings` - Generate embeddings

### Knowledge Management Endpoints

- `POST /jobs/session-summarize` - Background session summarization
- `GET /jobs/health` - Job service health check

### Health and Monitoring

- `GET /actuator/health` - Application health
- `GET /actuator/metrics` - Application metrics
- `GET /actuator/prometheus` - Prometheus metrics

## 🔐 Security

- API key authentication
- Multi-tenant data isolation
- Rate limiting per tenant
- Input validation and sanitization
- CORS configuration

## 📈 Monitoring

- Spring Boot Actuator endpoints
- Prometheus metrics
- Health checks
- Usage tracking and analytics

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request

## 📝 License

[Add your license information here]

## 🆘 Support

For issues and questions:
- Create an issue in the repository
- Check the documentation
- Review the test examples

## 🔄 Development Status

### ✅ Completed
- [x] Basic Spring Boot application structure
- [x] Core entity models (Tenant, ApiKey, KnowledgeObject, etc.)
- [x] Database migrations (PostgreSQL and H2)
- [x] Docker Compose development environment
- [x] Basic configuration profiles
- [x] Spring Security setup
- [x] Health check endpoints
- [x] Basic test framework

### 🔄 In Progress
- [ ] Service layer implementation
- [ ] API controllers
- [ ] Vector store integration
- [ ] LLM provider adapters
- [ ] Context building service
- [ ] Memory extraction service
- [ ] Usage tracking service

### 📋 Planned
- [ ] Background job services
- [ ] Oracle ADB integration
- [ ] OCI Object Storage integration
- [ ] Comprehensive test suite
- [ ] API documentation
- [ ] Deployment scripts
- [ ] Monitoring and observability
- [ ] Performance optimization

## 🎯 Next Steps

1. **Implement Core Services**: Complete the service layer implementation
2. **Add API Controllers**: Create OpenAI-compatible endpoints
3. **Vector Store Integration**: Implement PostgreSQL pgvector adapter
4. **LLM Provider Adapters**: Add OpenAI, Ollama, and mock adapters
5. **Comprehensive Testing**: Add unit and integration tests
6. **Documentation**: Complete API documentation
7. **Deployment**: Create production deployment scripts

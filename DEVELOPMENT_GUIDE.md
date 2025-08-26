# Development Guide for Knowledge-Aware LLM Middleware

## Overview
This guide provides comprehensive information for LLM coding assistants and developers working on the Knowledge-Aware LLM Middleware project. It covers architecture, coding standards, testing strategies, and development workflows.

## Project Architecture

### Core Components
1. **Spring Boot Application** (`KnowledgeMiddlewareApplication.java`)
   - Main entry point with `@EnableAsync` and `@EnableScheduling`
   - Located in `src/main/java/middleware/`

2. **Data Model** (`src/main/java/middleware/model/`)
   - **Entities**: `Tenant`, `ApiKey`, `KnowledgeObject`, `ContentVariant`, `KnowledgeRelationship`, `DialogueState`, `UsageLog`
   - **Enums**: `KnowledgeObjectType`, `ContentVariantType`, `TenantPlan`, `RelationshipType`
   - **ID Strategy**: Mixed approach - String IDs for most entities, UUID for relationships

3. **Repository Layer** (`src/main/java/middleware/repository/`)
   - JPA repositories extending `JpaRepository`
   - Custom query methods for complex operations
   - Tenant-aware queries for multi-tenancy

4. **Service Layer** (`src/main/java/middleware/service/`)
   - Interface definitions for all business logic
   - Implementations in `src/main/java/middleware/service/impl/`
   - Dependency injection via Spring

5. **API Controllers** (`src/main/java/middleware/api/`)
   - REST endpoints following OpenAI-compatible patterns
   - Security integration via Spring Security
   - Multi-tenant request handling

### Key Design Patterns

#### 1. Multi-Tenancy
- All entities include `tenantId` field
- Repository queries filter by tenant
- Security context provides tenant information
- API endpoints validate tenant access

#### 2. Service Interfaces
```java
public interface ServiceName {
    // Define contract
    ReturnType methodName(Parameters params);
}
```

#### 3. Implementation Classes
```java
@Service
public class ServiceNameImpl implements ServiceName {
    private final DependencyService dependencyService;
    
    @Autowired
    public ServiceNameImpl(DependencyService dependencyService) {
        this.dependencyService = dependencyService;
    }
    
    @Override
    public ReturnType methodName(Parameters params) {
        // Implementation
    }
}
```

## Database Schema

### Core Tables
1. **tenants** - Multi-tenant isolation
2. **api_keys** - Authentication and authorization
3. **knowledge_objects** - Core knowledge entities
4. **content_variants** - Different representations of knowledge
5. **knowledge_relationships** - Graph relationships between objects
6. **dialogue_states** - Conversation context and state
7. **usage_logs** - Token usage and cost tracking

### Migration Strategy
- **Flyway** for database migrations
- Separate scripts for PostgreSQL (`postgres/`) and H2 (`h2/`)
- Versioned migration files (V1__, V2__, etc.)
- PostgreSQL uses `vector(384)` for embeddings
- H2 uses `CLOB` for embeddings (simplified)

## Configuration Management

### Profiles
- **local** - Development environment
- **docker** - Docker Compose environment
- **test** - Unit and integration testing
- **gcp** - Google Cloud Platform deployment
- **production** - Production environment

### Key Configuration Files
- `application.yml` - Base configuration
- `application-{profile}.yml` - Profile-specific overrides
- Environment variables for sensitive data

## Development Standards

### Code Style
1. **Package Structure**
   ```
   middleware/
   ├── api/          # REST controllers
   ├── config/       # Configuration classes
   ├── model/        # JPA entities and DTOs
   ├── repository/   # Data access layer
   ├── service/      # Business logic interfaces
   └── service/impl/ # Business logic implementations
   ```

2. **Naming Conventions**
   - Classes: PascalCase (`KnowledgeObject`)
   - Methods: camelCase (`findByTenantId`)
   - Constants: UPPER_SNAKE_CASE (`MAX_TOKENS`)
   - Packages: lowercase (`middleware.service`)

3. **Annotations**
   - `@Service` for business logic
   - `@Repository` for data access
   - `@RestController` for API endpoints
   - `@Entity` for JPA entities
   - `@Autowired` for dependency injection

### Error Handling
1. **Global Exception Handler**
   - Centralized error handling
   - Consistent error responses
   - Proper HTTP status codes

2. **Service Layer Exceptions**
   - Custom exception types
   - Meaningful error messages
   - Proper exception propagation

### Logging
1. **SLF4J with Logback**
   - Structured logging
   - Appropriate log levels
   - Contextual information

2. **Log Levels**
   - `ERROR` - Application errors
   - `WARN` - Potential issues
   - `INFO` - Important events
   - `DEBUG` - Detailed debugging
   - `TRACE` - Very detailed debugging

## Testing Strategy

### Test Types
1. **Unit Tests** (`src/test/java/`)
   - Service layer testing
   - Repository layer testing
   - Mock external dependencies
   - Fast execution

2. **Integration Tests**
   - API endpoint testing
   - Database integration
   - Spring context loading
   - Use `@SpringBootTest`

3. **Test Configuration**
   - H2 in-memory database
   - Test profiles
   - Mock external services

### Test Naming
- `{ClassName}Test` for unit tests
- `{ClassName}IntegrationTest` for integration tests
- Descriptive test method names

### Test Data
- Use `@TestConfiguration` for test beans
- `@TestPropertySource` for test properties
- Test data builders for complex objects

## API Design

### REST Endpoints
1. **OpenAI-Compatible API**
   - `/v1/chat/completions` - Chat completions
   - `/v1/embeddings` - Embedding generation
   - `/v1/models` - Available models

2. **Management API**
   - `/api/tenants` - Tenant management
   - `/api/objects` - Knowledge object management
   - `/jobs/*` - Background job endpoints

### Request/Response Patterns
1. **Standard Response Format**
   ```json
   {
     "status": "success|error",
     "data": {...},
     "message": "Optional message",
     "timestamp": "2024-01-01T00:00:00Z"
   }
   ```

2. **Error Response Format**
   ```json
   {
     "status": "error",
     "error": {
       "code": "ERROR_CODE",
       "message": "Human readable message",
       "details": {...}
     }
   }
   ```

## Security Implementation

### Authentication
- API key-based authentication
- Multi-tenant isolation
- Rate limiting per tenant

### Authorization
- Role-based access control
- Resource-level permissions
- Tenant boundary enforcement

## Performance Considerations

### Caching Strategy
1. **Redis** - Distributed caching
2. **Caffeine** - Local caching
3. **Cache annotations** - Method-level caching

### Database Optimization
1. **Indexes** - Strategic indexing
2. **Query optimization** - Efficient queries
3. **Connection pooling** - HikariCP

### Rate Limiting
1. **Bucket4j** - Token bucket algorithm
2. **Per-tenant limits** - Isolated rate limiting
3. **Configurable limits** - Environment-based configuration

## Deployment

### Docker
- Multi-stage Dockerfile
- Health checks
- Non-root user
- Optimized layers

### Docker Compose
- Local development environment
- Service orchestration
- Environment variables
- Volume mounts

### Cloud Deployment
- Google Cloud Platform ready
- Kubernetes manifests
- Environment-specific configurations

## Monitoring and Observability

### Health Checks
- `/actuator/health` - Application health
- `/actuator/info` - Application information
- Custom health indicators

### Metrics
- Micrometer integration
- Prometheus metrics
- Custom business metrics

### Logging
- Structured logging
- Log aggregation ready
- Correlation IDs

## Development Workflow

### Git Workflow
1. **Feature branches** - `feature/feature-name`
2. **Bug fixes** - `fix/bug-description`
3. **Hotfixes** - `hotfix/urgent-fix`
4. **Releases** - `release/version`

### Code Review
1. **Pull requests** - Required for all changes
2. **Automated tests** - Must pass
3. **Code coverage** - Minimum thresholds
4. **Documentation** - Updated as needed

### Continuous Integration
1. **Build verification** - Maven build
2. **Test execution** - Unit and integration tests
3. **Code quality** - Static analysis
4. **Security scanning** - Dependency vulnerabilities

## Common Development Tasks

### Adding a New Service
1. Create interface in `service/` package
2. Create implementation in `service/impl/` package
3. Add `@Service` annotation
4. Create unit tests
5. Update documentation

### Adding a New API Endpoint
1. Create controller method
2. Add request/response DTOs
3. Implement service calls
4. Add integration tests
5. Update API documentation

### Database Schema Changes
1. Create Flyway migration script
2. Update entity classes
3. Update repository queries
4. Add integration tests
5. Test migration rollback

### Adding New Dependencies
1. Add to `pom.xml`
2. Update version management
3. Test compatibility
4. Update documentation
5. Security review

## Troubleshooting

### Common Issues
1. **Database Connection** - Check credentials and network
2. **Redis Connection** - Verify Redis is running
3. **Port Conflicts** - Check port availability
4. **Memory Issues** - Adjust JVM heap settings

### Debugging
1. **Enable DEBUG logging** - Set log level to DEBUG
2. **Use breakpoints** - IDE debugging
3. **Check logs** - Application and system logs
4. **Health endpoints** - Verify service health

### Performance Issues
1. **Database queries** - Check query performance
2. **Memory usage** - Monitor heap usage
3. **Network latency** - Check external service calls
4. **Cache hit rates** - Monitor cache effectiveness

## Resources

### Documentation
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Flyway Documentation](https://flywaydb.org/documentation/)
- [Docker Documentation](https://docs.docker.com/)

### Tools
- **IDE**: IntelliJ IDEA, Eclipse, VS Code
- **Database**: PostgreSQL, H2
- **Cache**: Redis, Caffeine
- **Testing**: JUnit 5, Mockito, TestContainers
- **Build**: Maven
- **Container**: Docker, Docker Compose

This guide should be updated as the project evolves and new patterns emerge.

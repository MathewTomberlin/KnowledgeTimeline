# LLM Coding Assistance Guide - Knowledge-Aware LLM Middleware

## Overview
This guide is specifically designed for LLM coding assistants (like GitHub Copilot, Cursor AI, etc.) to understand and effectively work with the Knowledge-Aware LLM Middleware project. It provides context, patterns, and guidelines for maintaining code quality and consistency.

## Project Context

### What This Project Is
The Knowledge-Aware LLM Middleware is a Spring Boot application that provides an OpenAI-compatible API with knowledge-aware capabilities. It acts as a middleware layer between LLM applications and various knowledge sources, enabling:

- **Knowledge Retrieval**: Semantic search through stored knowledge objects
- **Context Building**: Intelligent context assembly for LLM prompts
- **Relationship Discovery**: Automatic discovery of connections between knowledge
- **Multi-tenancy**: Isolated knowledge spaces per tenant
- **Usage Tracking**: Token usage and cost monitoring

### Key Architecture Principles
1. **Multi-tenant**: All data is tenant-scoped
2. **Service-oriented**: Clear separation of concerns with service interfaces
3. **Database-agnostic**: Support for PostgreSQL (with pgvector) and H2
4. **LLM-agnostic**: Support for OpenAI, Ollama, and other LLM providers
5. **Observable**: Comprehensive logging, metrics, and health checks

## Code Organization

### Package Structure
```
src/main/java/middleware/
├── KnowledgeMiddlewareApplication.java    # Main application class
├── api/                                   # REST controllers
├── config/                                # Configuration classes
├── model/                                 # JPA entities and DTOs
├── repository/                            # Data access layer
├── service/                               # Business logic interfaces
└── service/impl/                          # Business logic implementations
```

### Key Files to Understand
- **`pom.xml`**: Maven dependencies and build configuration
- **`application.yml`**: Base Spring Boot configuration
- **`application-{profile}.yml`**: Environment-specific configurations
- **`db/migration/`**: Database schema migrations
- **`docker-compose.yml`**: Local development environment

## Coding Patterns and Conventions

### 1. Entity Design Pattern
All entities follow this pattern:
```java
@Entity
@Table(name = "table_name")
public class EntityName {
    @Id
    private String id;  // String ID for most entities
    
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;  // Multi-tenant isolation
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Getters, setters, equals, hashCode, toString
}
```

### 2. Repository Pattern
```java
@Repository
public interface EntityRepository extends JpaRepository<Entity, String> {
    // Tenant-scoped queries
    List<Entity> findByTenantId(String tenantId);
    
    // Custom queries with @Query annotation
    @Query("SELECT e FROM Entity e WHERE e.tenantId = :tenantId AND e.active = true")
    List<Entity> findActiveByTenantId(@Param("tenantId") String tenantId);
}
```

### 3. Service Pattern
```java
public interface EntityService {
    Entity createEntity(String tenantId, CreateEntityRequest request);
    Entity getEntity(String tenantId, String entityId);
    Page<Entity> getEntities(String tenantId, Pageable pageable);
    Entity updateEntity(String tenantId, String entityId, UpdateEntityRequest request);
    void deleteEntity(String tenantId, String entityId);
}

@Service
public class EntityServiceImpl implements EntityService {
    private final EntityRepository repository;
    private final OtherService otherService;
    
    @Autowired
    public EntityServiceImpl(EntityRepository repository, OtherService otherService) {
        this.repository = repository;
        this.otherService = otherService;
    }
    
    @Override
    @Transactional
    public Entity createEntity(String tenantId, CreateEntityRequest request) {
        // Validation
        validateRequest(request);
        
        // Business logic
        Entity entity = new Entity();
        entity.setId(UUID.randomUUID().toString());
        entity.setTenantId(tenantId);
        // ... set other fields
        
        // Save
        return repository.save(entity);
    }
}
```

### 4. Controller Pattern
```java
@RestController
@RequestMapping("/api/entities")
public class EntityController {
    private final EntityService entityService;
    
    @Autowired
    public EntityController(EntityService entityService) {
        this.entityService = entityService;
    }
    
    @GetMapping
    public ResponseEntity<Page<Entity>> getEntities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        
        String tenantId = authentication.getName();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Entity> entities = entityService.getEntities(tenantId, pageable);
        
        return ResponseEntity.ok(entities);
    }
    
    @PostMapping
    public ResponseEntity<Entity> createEntity(
            @RequestBody @Valid CreateEntityRequest request,
            Authentication authentication) {
        
        String tenantId = authentication.getName();
        Entity entity = entityService.createEntity(tenantId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(entity);
    }
}
```

## Database Schema Understanding

### Core Tables
1. **`tenants`**: Multi-tenant isolation
2. **`api_keys`**: Authentication and authorization
3. **`knowledge_objects`**: Core knowledge entities
4. **`content_variants`**: Different representations of knowledge
5. **`knowledge_relationships`**: Graph relationships between objects
6. **`dialogue_states`**: Conversation context and state
7. **`usage_logs`**: Token usage and cost tracking

### ID Strategy
- **String IDs**: Most entities use `VARCHAR(36)` for UUID strings
- **UUID IDs**: Relationships use actual `UUID` type
- **Auto-generation**: IDs are generated in Java code, not database

### Key Relationships
- All entities have `tenant_id` for multi-tenancy
- `knowledge_objects` can have multiple `content_variants`
- `knowledge_relationships` link objects with `source_id` and `target_id`
- `api_keys` belong to `tenants`

## Configuration Management

### Profiles
- **`local`**: Development environment
- **`docker`**: Docker Compose environment
- **`test`**: Unit and integration testing
- **`gcp`**: Google Cloud Platform deployment
- **`production`**: Production environment

### Key Configuration Properties
```yaml
spring:
  datasource:
    url: jdbc:postgresql://host:port/database
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  redis:
    host: ${REDIS_HOST}
    port: 6379
  jpa:
    hibernate:
      ddl-auto: validate  # Never use create-drop in production
    show-sql: false

llm:
  service:
    url: ${LLM_SERVICE_URL}
    type: ${LLM_SERVICE_TYPE}
  embedding:
    service:
      url: ${EMBEDDING_SERVICE_URL}
      type: ${EMBEDDING_SERVICE_TYPE}
```

## Testing Patterns

### Unit Test Pattern
```java
@ExtendWith(MockitoExtension.class)
class EntityServiceTest {
    @Mock
    private EntityRepository repository;
    
    @InjectMocks
    private EntityServiceImpl service;
    
    @Test
    void createEntity_Success() {
        // Arrange
        CreateEntityRequest request = CreateEntityRequest.builder()
            .name("Test Entity")
            .build();
        
        Entity expectedEntity = new Entity();
        expectedEntity.setId("test-id");
        expectedEntity.setName("Test Entity");
        
        when(repository.save(any(Entity.class))).thenReturn(expectedEntity);
        
        // Act
        Entity result = service.createEntity("tenant-123", request);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Entity");
        verify(repository).save(any(Entity.class));
    }
}
```

### Integration Test Pattern
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class EntityIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private EntityRepository repository;
    
    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }
    
    @Test
    void createAndRetrieveEntity_Success() {
        // Test full API flow
    }
}
```

## Common Development Tasks

### Adding a New Entity
1. **Create Entity Class**:
   ```java
   @Entity
   @Table(name = "new_entity")
   public class NewEntity {
       @Id
       private String id;
       
       @Column(name = "tenant_id", nullable = false)
       private String tenantId;
       
       @Column(name = "name", nullable = false)
       private String name;
       
       // ... other fields, getters, setters
   }
   ```

2. **Create Repository**:
   ```java
   @Repository
   public interface NewEntityRepository extends JpaRepository<NewEntity, String> {
       List<NewEntity> findByTenantId(String tenantId);
   }
   ```

3. **Create Service**:
   ```java
   public interface NewEntityService {
       NewEntity createNewEntity(String tenantId, CreateNewEntityRequest request);
       // ... other methods
   }
   
   @Service
   public class NewEntityServiceImpl implements NewEntityService {
       // Implementation
   }
   ```

4. **Create Controller**:
   ```java
   @RestController
   @RequestMapping("/api/new-entities")
   public class NewEntityController {
       // REST endpoints
   }
   ```

5. **Add Migration**:
   ```sql
   -- V2__add_new_entity.sql
   CREATE TABLE new_entity (
       id VARCHAR(36) PRIMARY KEY,
       tenant_id VARCHAR(36) NOT NULL,
       name VARCHAR(255) NOT NULL,
       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
       updated_at TIMESTAMP
   );
   
   CREATE INDEX idx_new_entity_tenant_id ON new_entity(tenant_id);
   ```

### Adding a New API Endpoint
1. **Define Request/Response DTOs**:
   ```java
   public class NewEndpointRequest {
       @NotBlank
       private String field;
       // ... getters, setters
   }
   
   public class NewEndpointResponse {
       private String result;
       // ... getters, setters
   }
   ```

2. **Add Service Method**:
   ```java
   public interface ExistingService {
       NewEndpointResponse processNewEndpoint(String tenantId, NewEndpointRequest request);
   }
   ```

3. **Add Controller Method**:
   ```java
   @PostMapping("/new-endpoint")
   public ResponseEntity<NewEndpointResponse> newEndpoint(
           @RequestBody @Valid NewEndpointRequest request,
           Authentication authentication) {
       
       String tenantId = authentication.getName();
       NewEndpointResponse response = service.processNewEndpoint(tenantId, request);
       
       return ResponseEntity.ok(response);
   }
   ```

4. **Add Tests**:
   ```java
   @Test
   void newEndpoint_Success() {
       // Test implementation
   }
   ```

## Error Handling Patterns

### Service Layer Exceptions
```java
public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String entityId) {
        super("Entity not found: " + entityId);
    }
}

public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
```

### Global Exception Handler
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException e) {
        ErrorResponse error = new ErrorResponse("NOT_FOUND", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException e) {
        ErrorResponse error = new ErrorResponse("INVALID_REQUEST", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
```

## Security Patterns

### Authentication
- API key-based authentication
- Keys are hashed before storage
- Tenant isolation enforced at service layer

### Authorization
- All endpoints require valid API key
- Tenant access validated on every request
- Resource-level permissions (future enhancement)

## Performance Considerations

### Caching
```java
@Cacheable(value = "entities", key = "#tenantId + ':' + #entityId")
public Entity getEntity(String tenantId, String entityId) {
    // Implementation
}
```

### Database Optimization
- Use indexes on frequently queried columns
- Implement pagination for large result sets
- Use appropriate data types (TEXT for large content)

### Rate Limiting
- Per-tenant rate limiting
- Configurable limits based on plan
- Token bucket algorithm implementation

## Monitoring and Observability

### Logging
```java
private static final Logger logger = LoggerFactory.getLogger(ServiceName.class);

logger.info("Processing request for tenant: {}", tenantId);
logger.error("Error processing request: {}", e.getMessage(), e);
```

### Metrics
```java
@Timed("knowledge.object.creation")
@Counted("knowledge.object.creation.attempts")
public KnowledgeObject createKnowledgeObject(String tenantId, CreateRequest request) {
    // Implementation
}
```

### Health Checks
```java
@Component
public class CustomHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        // Check external service health
        return Health.up().withDetail("service", "healthy").build();
    }
}
```

## Common Pitfalls to Avoid

### 1. Missing Tenant Isolation
❌ **Wrong**:
```java
public List<Entity> getAllEntities() {
    return repository.findAll(); // No tenant filtering
}
```

✅ **Correct**:
```java
public List<Entity> getAllEntities(String tenantId) {
    return repository.findByTenantId(tenantId);
}
```

### 2. Not Handling UUID/String Conversions
❌ **Wrong**:
```java
// Mixing UUID and String IDs
UUID sourceId = UUID.fromString(knowledgeObject.getId());
```

✅ **Correct**:
```java
// Be consistent with ID types
String sourceId = knowledgeObject.getId();
```

### 3. Missing Validation
❌ **Wrong**:
```java
public Entity createEntity(CreateRequest request) {
    Entity entity = new Entity();
    entity.setName(request.getName()); // No validation
    return repository.save(entity);
}
```

✅ **Correct**:
```java
public Entity createEntity(String tenantId, CreateRequest request) {
    validateRequest(request);
    
    Entity entity = new Entity();
    entity.setId(UUID.randomUUID().toString());
    entity.setTenantId(tenantId);
    entity.setName(request.getName());
    return repository.save(entity);
}

private void validateRequest(CreateRequest request) {
    if (request.getName() == null || request.getName().trim().isEmpty()) {
        throw new ValidationException("Name cannot be empty");
    }
}
```

### 4. Not Using Transactions
❌ **Wrong**:
```java
public void processComplexOperation() {
    // Multiple database operations without transaction
    repository.save(entity1);
    repository.save(entity2);
    // If this fails, entity1 is still saved
}
```

✅ **Correct**:
```java
@Transactional
public void processComplexOperation() {
    // All operations in single transaction
    repository.save(entity1);
    repository.save(entity2);
}
```

## Best Practices Summary

### Code Quality
1. **Follow naming conventions**: camelCase for methods, PascalCase for classes
2. **Use meaningful names**: Avoid abbreviations and unclear names
3. **Keep methods small**: Single responsibility principle
4. **Add comprehensive tests**: Unit tests for all business logic
5. **Document complex logic**: Add comments for non-obvious code

### Security
1. **Always validate input**: Check all user inputs
2. **Enforce tenant isolation**: Never mix data between tenants
3. **Use parameterized queries**: Prevent SQL injection
4. **Hash sensitive data**: Never store plain text passwords or API keys
5. **Log security events**: Track authentication and authorization

### Performance
1. **Use pagination**: For large result sets
2. **Implement caching**: For frequently accessed data
3. **Optimize database queries**: Use indexes and efficient queries
4. **Monitor resource usage**: Track memory, CPU, and database performance
5. **Use async operations**: For long-running tasks

### Maintainability
1. **Follow SOLID principles**: Especially dependency injection
2. **Use interfaces**: For service contracts
3. **Keep configuration external**: Use environment variables
4. **Version your APIs**: Maintain backward compatibility
5. **Document changes**: Update documentation with code changes

## Quick Reference

### Common Annotations
- `@Entity`: JPA entity
- `@Repository`: Data access layer
- `@Service`: Business logic
- `@RestController`: REST API endpoints
- `@Autowired`: Dependency injection
- `@Transactional`: Database transaction
- `@Cacheable`: Method caching
- `@Timed`: Performance monitoring

### Common Dependencies
- `spring-boot-starter-web`: Web application
- `spring-boot-starter-data-jpa`: Database access
- `spring-boot-starter-security`: Security framework
- `spring-boot-starter-data-redis`: Redis caching
- `postgresql`: PostgreSQL driver
- `flyway-core`: Database migrations

### Common Commands
```bash
# Build project
./mvnw clean package

# Run tests
./mvnw test

# Start with profile
java -jar target/knowledge-middleware-1.0.0.jar --spring.profiles.active=local

# Docker compose
docker-compose up -d

# Database migration
./mvnw flyway:migrate
```

This guide should be updated as the project evolves and new patterns emerge. Always refer to the latest documentation and follow established patterns in the codebase.

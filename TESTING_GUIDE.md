# Testing Guide - Knowledge-Aware LLM Middleware

## Overview
This guide provides comprehensive testing strategies, patterns, and examples for the Knowledge-Aware LLM Middleware project. It covers unit testing, integration testing, API testing, and performance testing.

## Testing Strategy

### Test Pyramid
```
    /\
   /  \     E2E Tests (Few)
  /____\    Integration Tests (Some)
 /______\   Unit Tests (Many)
```

### Test Types
1. **Unit Tests** - Fast, isolated, test individual components
2. **Integration Tests** - Test component interactions
3. **API Tests** - Test REST endpoints
4. **Performance Tests** - Test scalability and performance
5. **Security Tests** - Test authentication and authorization

## Unit Testing

### Service Layer Testing

#### Example: KnowledgeObjectService Test
```java
@ExtendWith(MockitoExtension.class)
class KnowledgeObjectServiceTest {

    @Mock
    private KnowledgeObjectRepository repository;
    
    @Mock
    private EmbeddingService embeddingService;
    
    @InjectMocks
    private KnowledgeObjectServiceImpl service;

    @Test
    void createKnowledgeObject_Success() {
        // Arrange
        CreateKnowledgeObjectRequest request = CreateKnowledgeObjectRequest.builder()
            .type(KnowledgeObjectType.DOCUMENT)
            .title("Test Document")
            .content("Test content")
            .build();
            
        KnowledgeObject expectedObject = new KnowledgeObject();
        expectedObject.setId("obj-123");
        expectedObject.setTitle("Test Document");
        
        when(repository.save(any(KnowledgeObject.class))).thenReturn(expectedObject);
        when(embeddingService.generateEmbedding(anyString())).thenReturn(new float[384]);

        // Act
        KnowledgeObject result = service.createKnowledgeObject("tenant-123", request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Document");
        verify(repository).save(any(KnowledgeObject.class));
        verify(embeddingService).generateEmbedding("Test content");
    }

    @Test
    void createKnowledgeObject_InvalidRequest_ThrowsException() {
        // Arrange
        CreateKnowledgeObjectRequest request = CreateKnowledgeObjectRequest.builder()
            .type(KnowledgeObjectType.DOCUMENT)
            .title("") // Invalid empty title
            .content("Test content")
            .build();

        // Act & Assert
        assertThatThrownBy(() -> service.createKnowledgeObject("tenant-123", request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Title cannot be empty");
    }
}
```

#### Example: Repository Layer Test
```java
@DataJpaTest
@ActiveProfiles("test")
class KnowledgeObjectRepositoryTest {

    @Autowired
    private KnowledgeObjectRepository repository;
    
    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByTenantId_ReturnsOnlyTenantObjects() {
        // Arrange
        KnowledgeObject obj1 = createKnowledgeObject("tenant-1", "Object 1");
        KnowledgeObject obj2 = createKnowledgeObject("tenant-1", "Object 2");
        KnowledgeObject obj3 = createKnowledgeObject("tenant-2", "Object 3");
        
        entityManager.persistAndFlush(obj1);
        entityManager.persistAndFlush(obj2);
        entityManager.persistAndFlush(obj3);

        // Act
        List<KnowledgeObject> results = repository.findByTenantId("tenant-1");

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results).extracting("title")
            .containsExactlyInAnyOrder("Object 1", "Object 2");
    }

    @Test
    void findRecentObjectsWithoutRelationships_ReturnsCorrectObjects() {
        // Arrange
        KnowledgeObject obj1 = createKnowledgeObject("tenant-1", "Object 1");
        KnowledgeObject obj2 = createKnowledgeObject("tenant-1", "Object 2");
        
        entityManager.persistAndFlush(obj1);
        entityManager.persistAndFlush(obj2);

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        List<KnowledgeObject> results = repository.findRecentObjectsWithoutRelationships("tenant-1", pageable);

        // Assert
        assertThat(results).hasSize(2);
    }

    private KnowledgeObject createKnowledgeObject(String tenantId, String title) {
        KnowledgeObject obj = new KnowledgeObject();
        obj.setId(UUID.randomUUID().toString());
        obj.setTenantId(tenantId);
        obj.setType(KnowledgeObjectType.DOCUMENT);
        obj.setTitle(title);
        obj.setContent("Test content");
        obj.setCreatedAt(LocalDateTime.now());
        return obj;
    }
}
```

### Controller Layer Testing

#### Example: API Controller Test
```java
@WebMvcTest(KnowledgeObjectController.class)
@ActiveProfiles("test")
class KnowledgeObjectControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private KnowledgeObjectService service;
    
    @MockBean
    private ApiKeyService apiKeyService;

    @Test
    void getKnowledgeObjects_Success() throws Exception {
        // Arrange
        List<KnowledgeObject> objects = Arrays.asList(
            createKnowledgeObject("obj-1", "Object 1"),
            createKnowledgeObject("obj-2", "Object 2")
        );
        
        Page<KnowledgeObject> page = new PageImpl<>(objects);
        when(service.getKnowledgeObjects(anyString(), any(), any())).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/objects")
                .header("Authorization", "Bearer test-api-key")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.content[0].title").value("Object 1"));
    }

    @Test
    void getKnowledgeObjects_Unauthorized_Returns401() throws Exception {
        // Arrange
        when(apiKeyService.validateApiKey("invalid-key")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/objects")
                .header("Authorization", "Bearer invalid-key"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void createKnowledgeObject_Success() throws Exception {
        // Arrange
        CreateKnowledgeObjectRequest request = CreateKnowledgeObjectRequest.builder()
            .type(KnowledgeObjectType.DOCUMENT)
            .title("New Object")
            .content("Test content")
            .build();
            
        KnowledgeObject createdObject = createKnowledgeObject("obj-123", "New Object");
        when(service.createKnowledgeObject(anyString(), any())).thenReturn(createdObject);

        // Act & Assert
        mockMvc.perform(post("/api/objects")
                .header("Authorization", "Bearer test-api-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value("obj-123"))
            .andExpect(jsonPath("$.title").value("New Object"));
    }

    private KnowledgeObject createKnowledgeObject(String id, String title) {
        KnowledgeObject obj = new KnowledgeObject();
        obj.setId(id);
        obj.setTitle(title);
        obj.setType(KnowledgeObjectType.DOCUMENT);
        obj.setContent("Test content");
        obj.setCreatedAt(LocalDateTime.now());
        return obj;
    }
}
```

## Integration Testing

### Full Application Context Testing

#### Example: Integration Test
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class KnowledgeObjectIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private KnowledgeObjectRepository repository;
    
    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        apiKeyRepository.deleteAll();
        
        // Create test API key
        ApiKey apiKey = new ApiKey();
        apiKey.setId("key-123");
        apiKey.setTenantId("tenant-123");
        apiKey.setKeyHash("test-key-hash");
        apiKey.setActive(true);
        apiKeyRepository.save(apiKey);
    }

    @Test
    void createAndRetrieveKnowledgeObject_Success() {
        // Arrange
        CreateKnowledgeObjectRequest request = CreateKnowledgeObjectRequest.builder()
            .type(KnowledgeObjectType.DOCUMENT)
            .title("Integration Test Object")
            .content("Test content for integration")
            .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("test-key-hash");
        HttpEntity<CreateKnowledgeObjectRequest> entity = new HttpEntity<>(request, headers);

        // Act - Create object
        ResponseEntity<KnowledgeObject> createResponse = restTemplate.exchange(
            "/api/objects",
            HttpMethod.POST,
            entity,
            KnowledgeObject.class
        );

        // Assert - Creation
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().getTitle()).isEqualTo("Integration Test Object");

        String objectId = createResponse.getBody().getId();

        // Act - Retrieve object
        ResponseEntity<KnowledgeObject> getResponse = restTemplate.exchange(
            "/api/objects/" + objectId,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            KnowledgeObject.class
        );

        // Assert - Retrieval
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().getId()).isEqualTo(objectId);
    }

    @Test
    void listKnowledgeObjects_WithPagination_Success() {
        // Arrange
        createTestObjects(5);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("test-key-hash");

        // Act
        ResponseEntity<Page<KnowledgeObject>> response = restTemplate.exchange(
            "/api/objects?page=0&size=3",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            new ParameterizedTypeReference<Page<KnowledgeObject>>() {}
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(3);
        assertThat(response.getBody().getTotalElements()).isEqualTo(5);
        assertThat(response.getBody().getTotalPages()).isEqualTo(2);
    }

    private void createTestObjects(int count) {
        for (int i = 0; i < count; i++) {
            KnowledgeObject obj = new KnowledgeObject();
            obj.setId(UUID.randomUUID().toString());
            obj.setTenantId("tenant-123");
            obj.setType(KnowledgeObjectType.DOCUMENT);
            obj.setTitle("Test Object " + i);
            obj.setContent("Content " + i);
            obj.setCreatedAt(LocalDateTime.now());
            repository.save(obj);
        }
    }
}
```

### Database Integration Testing

#### Example: Repository Integration Test
```java
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class KnowledgeObjectRepositoryIntegrationTest {

    @Autowired
    private KnowledgeObjectRepository repository;
    
    @Autowired
    private KnowledgeRelationshipRepository relationshipRepository;

    @Test
    void findRecentObjectsWithoutRelationships_Integration() {
        // Arrange
        KnowledgeObject obj1 = createKnowledgeObject("obj-1");
        KnowledgeObject obj2 = createKnowledgeObject("obj-2");
        KnowledgeObject obj3 = createKnowledgeObject("obj-3");
        
        repository.saveAll(Arrays.asList(obj1, obj2, obj3));
        
        // Create a relationship for obj1
        KnowledgeRelationship rel = new KnowledgeRelationship();
        rel.setId(UUID.randomUUID());
        rel.setSourceId(UUID.fromString(obj1.getId()));
        rel.setTargetId(UUID.fromString(obj2.getId()));
        rel.setType(RelationshipType.RELATES_TO);
        rel.setStrength(0.8);
        relationshipRepository.save(rel);

        Pageable pageable = PageRequest.of(0, 10);

        // Act
        List<KnowledgeObject> results = repository.findRecentObjectsWithoutRelationships("tenant-123", pageable);

        // Assert
        assertThat(results).hasSize(1); // Only obj3 should be returned
        assertThat(results.get(0).getId()).isEqualTo("obj-3");
    }

    private KnowledgeObject createKnowledgeObject(String id) {
        KnowledgeObject obj = new KnowledgeObject();
        obj.setId(id);
        obj.setTenantId("tenant-123");
        obj.setType(KnowledgeObjectType.DOCUMENT);
        obj.setTitle("Test Object " + id);
        obj.setContent("Test content");
        obj.setCreatedAt(LocalDateTime.now());
        return obj;
    }
}
```

## API Testing

### OpenAI-Compatible API Testing

#### Example: Chat Completions API Test
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ChatCompletionsApiTest {

    @Autowired
    private TestRestTemplate restTemplate;
    
    @MockBean
    private LLMClientService llmClientService;
    
    @MockBean
    private ContextBuilderService contextBuilderService;

    @Test
    void chatCompletions_WithKnowledgeContext_Success() {
        // Arrange
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("gpt-4")
            .messages(Arrays.asList(
                new ChatMessage("user", "What do you know about machine learning?")
            ))
            .temperature(0.7)
            .maxTokens(1000)
            .knowledgeContext(KnowledgeContext.builder()
                .includeRecent(true)
                .includeRelated(true)
                .maxContextObjects(10)
                .similarityThreshold(0.8)
                .build())
            .build();

        ChatCompletionResponse mockResponse = ChatCompletionResponse.builder()
            .id("chatcmpl-123")
            .choices(Arrays.asList(
                new ChatChoice(0, new ChatMessage("assistant", "Based on our knowledge base..."), "stop")
            ))
            .usage(new TokenUsage(100, 200, 300))
            .build();

        when(llmClientService.createChatCompletion(any())).thenReturn(mockResponse);
        when(contextBuilderService.buildContext(any(), any())).thenReturn("Enhanced context");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("test-api-key");
        HttpEntity<ChatCompletionRequest> entity = new HttpEntity<>(request, headers);

        // Act
        ResponseEntity<ChatCompletionResponse> response = restTemplate.exchange(
            "/v1/chat/completions",
            HttpMethod.POST,
            entity,
            ChatCompletionResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo("chatcmpl-123");
        assertThat(response.getBody().getChoices()).hasSize(1);
        assertThat(response.getBody().getChoices().get(0).getMessage().getContent())
            .contains("Based on our knowledge base");
    }
}
```

### Background Job API Testing

#### Example: Job Endpoints Test
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class JobControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;
    
    @MockBean
    private SessionSummarizationService sessionSummarizationService;

    @Test
    void sessionSummarize_Success() {
        // Arrange
        when(sessionSummarizationService.summarizeSessions(anyString(), anyString(), anyString(), anyInt()))
            .thenReturn(5);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("test-api-key");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // Act
        ResponseEntity<Map> response = restTemplate.exchange(
            "/jobs/session-summarize?batch_size=100",
            HttpMethod.POST,
            entity,
            Map.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("success");
        assertThat(response.getBody().get("sessionsSummarized")).isEqualTo(5);
    }

    @Test
    void jobHealth_Success() {
        // Arrange
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("test-api-key");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // Act
        ResponseEntity<Map> response = restTemplate.exchange(
            "/jobs/health",
            HttpMethod.GET,
            entity,
            Map.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("healthy");
        assertThat(response.getBody().get("endpoints")).isNotNull();
    }
}
```

## Performance Testing

### Load Testing

#### Example: Performance Test
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PerformanceTest {

    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private KnowledgeObjectRepository repository;

    @Test
    void concurrentRequests_PerformanceTest() throws InterruptedException {
        // Arrange
        int numberOfThreads = 10;
        int requestsPerThread = 100;
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        createTestData(1000); // Pre-populate with test data

        // Act
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numberOfThreads; i++) {
            new Thread(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        try {
                            ResponseEntity<Page> response = restTemplate.exchange(
                                "/api/objects?page=0&size=20",
                                HttpMethod.GET,
                                new HttpEntity<>(createAuthHeaders()),
                                Page.class
                            );
                            
                            if (response.getStatusCode() == HttpStatus.OK) {
                                successCount.incrementAndGet();
                            } else {
                                errorCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        latch.await(30, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        // Assert
        long totalTime = endTime - startTime;
        int totalRequests = numberOfThreads * requestsPerThread;
        double requestsPerSecond = (double) totalRequests / (totalTime / 1000.0);
        
        System.out.println("Performance Test Results:");
        System.out.println("Total Requests: " + totalRequests);
        System.out.println("Successful Requests: " + successCount.get());
        System.out.println("Failed Requests: " + errorCount.get());
        System.out.println("Total Time: " + totalTime + "ms");
        System.out.println("Requests per Second: " + requestsPerSecond);
        
        assertThat(successCount.get()).isGreaterThan(totalRequests * 0.95); // 95% success rate
        assertThat(requestsPerSecond).isGreaterThan(100); // At least 100 RPS
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("test-api-key");
        return headers;
    }

    private void createTestData(int count) {
        List<KnowledgeObject> objects = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            KnowledgeObject obj = new KnowledgeObject();
            obj.setId(UUID.randomUUID().toString());
            obj.setTenantId("tenant-123");
            obj.setType(KnowledgeObjectType.DOCUMENT);
            obj.setTitle("Performance Test Object " + i);
            obj.setContent("Content for performance testing");
            obj.setCreatedAt(LocalDateTime.now());
            objects.add(obj);
        }
        repository.saveAll(objects);
    }
}
```

## Security Testing

### Authentication and Authorization Testing

#### Example: Security Test
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SecurityTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void accessWithoutAuth_ReturnsUnauthorized() {
        // Act
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/objects",
            HttpMethod.GET,
            new HttpEntity<>(new HttpHeaders()),
            String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void accessWithInvalidApiKey_ReturnsUnauthorized() {
        // Arrange
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("invalid-api-key");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/objects",
            HttpMethod.GET,
            entity,
            String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void accessWithValidApiKey_Success() {
        // Arrange
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("valid-api-key");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // Act
        ResponseEntity<Page> response = restTemplate.exchange(
            "/api/objects",
            HttpMethod.GET,
            entity,
            Page.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

## Test Configuration

### Test Properties
```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false
    properties:
      hibernate:
        format_sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration/h2
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms

logging:
  level:
    middleware: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE

# Test-specific configurations
test:
  api-key: test-api-key
  tenant-id: test-tenant-123
  mock-llm-enabled: true
  mock-embedding-enabled: true
```

### Test Utilities

#### Example: Test Data Builder
```java
public class TestDataBuilder {

    public static KnowledgeObject createKnowledgeObject(String tenantId, String title) {
        KnowledgeObject obj = new KnowledgeObject();
        obj.setId(UUID.randomUUID().toString());
        obj.setTenantId(tenantId);
        obj.setType(KnowledgeObjectType.DOCUMENT);
        obj.setTitle(title);
        obj.setContent("Test content for " + title);
        obj.setCreatedAt(LocalDateTime.now());
        obj.setMetadata("{\"source\": \"test\"}");
        return obj;
    }

    public static ApiKey createApiKey(String tenantId, String keyHash) {
        ApiKey apiKey = new ApiKey();
        apiKey.setId(UUID.randomUUID().toString());
        apiKey.setTenantId(tenantId);
        apiKey.setKeyHash(keyHash);
        apiKey.setActive(true);
        apiKey.setCreatedAt(LocalDateTime.now());
        return apiKey;
    }

    public static ChatCompletionRequest createChatRequest(String message) {
        return ChatCompletionRequest.builder()
            .model("gpt-4")
            .messages(Arrays.asList(new ChatMessage("user", message)))
            .temperature(0.7)
            .maxTokens(1000)
            .build();
    }
}
```

## Test Execution

### Running Tests
```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=KnowledgeObjectServiceTest

# Run tests with specific profile
./mvnw test -Dspring.profiles.active=test

# Run integration tests only
./mvnw test -Dtest=*IntegrationTest

# Run tests with coverage
./mvnw test jacoco:report
```

### Test Reports
- **Surefire Reports**: `target/surefire-reports/`
- **Jacoco Coverage**: `target/site/jacoco/`
- **Test Results**: `target/test-results/`

## Best Practices

### Test Organization
1. **Package Structure**: Mirror main package structure
2. **Naming**: `{ClassName}Test` for unit tests, `{ClassName}IntegrationTest` for integration tests
3. **Test Methods**: Descriptive names that explain the scenario
4. **Grouping**: Use `@Nested` for related test scenarios

### Test Data Management
1. **Isolation**: Each test should be independent
2. **Cleanup**: Use `@Transactional` and `@Rollback` for database tests
3. **Fixtures**: Create reusable test data builders
4. **Randomization**: Use random data to catch edge cases

### Mocking Strategy
1. **External Services**: Mock all external dependencies
2. **Database**: Use in-memory database for tests
3. **Time**: Mock time-dependent operations
4. **Configuration**: Use test-specific configuration

### Assertions
1. **Specific**: Use specific assertions rather than generic ones
2. **Descriptive**: Provide meaningful assertion messages
3. **Complete**: Test all relevant aspects of the response
4. **Edge Cases**: Test boundary conditions and error scenarios

This testing guide should be updated as the project evolves and new testing patterns emerge.

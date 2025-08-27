package middleware.service.impl;

import middleware.model.KnowledgeObject;
import middleware.model.ContentVariant;
import middleware.model.KnowledgeObjectType;
import middleware.model.ContentVariantType;
import middleware.repository.KnowledgeObjectRepository;
import middleware.repository.ContentVariantRepository;
import middleware.service.EmbeddingService;
import middleware.service.TokenCountingService;
import middleware.service.MemoryExtractionService.MemoryExtraction;
import middleware.service.MemoryExtractionService.Fact;
import middleware.service.MemoryExtractionService.Entity;
import middleware.service.MemoryExtractionService.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for KnowledgeObjectServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeObjectServiceImplTest {

    @Mock
    private KnowledgeObjectRepository knowledgeObjectRepository;

    @Mock
    private ContentVariantRepository contentVariantRepository;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private TokenCountingService tokenCountingService;

    @InjectMocks
    private KnowledgeObjectServiceImpl knowledgeObjectService;

    private static final String TENANT_ID = "tenant-123";
    private static final String SESSION_ID = "session-456";
    private static final String USER_ID = "user-789";
    private static final String USER_MESSAGE = "Hello, how are you?";
    private static final String ASSISTANT_MESSAGE = "I'm doing well, thank you for asking!";

    @BeforeEach
    void setUp() {
        // No global setup needed - each test will stub what it needs
    }

    @Test
    void testCreateUserTurn() {
        // Given
        Map<String, Object> metadata = Map.of("source", "chat", "timestamp", "2024-01-01");
        when(tokenCountingService.countTokens(USER_MESSAGE, "gpt-3.5-turbo")).thenReturn(10);

        // When
        KnowledgeObject result = knowledgeObjectService.createUserTurn(TENANT_ID, SESSION_ID, USER_ID, USER_MESSAGE, metadata);

        // Then
        assertNotNull(result);
        assertEquals(TENANT_ID, result.getTenantId());
        assertEquals(KnowledgeObjectType.TURN, result.getType());
        assertEquals(SESSION_ID, result.getSessionId());
        assertEquals(USER_ID, result.getUserId());
        assertNull(result.getParentId());
        assertEquals("[]", result.getTags());
        assertTrue(result.getMetadata().contains("source"));
        assertTrue(result.getMetadata().contains("timestamp"));
        assertFalse(result.isArchived());
        assertNotNull(result.getCreatedAt());
        assertEquals(10, result.getOriginalTokens());

        verify(tokenCountingService).countTokens(USER_MESSAGE, "gpt-3.5-turbo");
    }

    @Test
    void testCreateUserTurn_WithNullMetadata() {
        // Given
        when(tokenCountingService.countTokens(USER_MESSAGE, "gpt-3.5-turbo")).thenReturn(10);

        // When
        KnowledgeObject result = knowledgeObjectService.createUserTurn(TENANT_ID, SESSION_ID, USER_ID, USER_MESSAGE, null);

        // Then
        assertNotNull(result);
        assertEquals("{}", result.getMetadata());
    }

    @Test
    void testCreateUserTurn_WithEmptyMetadata() {
        // Given
        when(tokenCountingService.countTokens(USER_MESSAGE, "gpt-3.5-turbo")).thenReturn(10);

        // When
        KnowledgeObject result = knowledgeObjectService.createUserTurn(TENANT_ID, SESSION_ID, USER_ID, USER_MESSAGE, new HashMap<>());

        // Then
        assertNotNull(result);
        assertEquals("{}", result.getMetadata());
    }

    @Test
    void testCreateAssistantTurn() {
        // Given
        Map<String, Object> metadata = Map.of("source", "llm", "model", "gpt-3.5-turbo");
        when(tokenCountingService.countTokens(ASSISTANT_MESSAGE, "gpt-3.5-turbo")).thenReturn(10);

        // When
        KnowledgeObject result = knowledgeObjectService.createAssistantTurn(TENANT_ID, SESSION_ID, USER_ID, ASSISTANT_MESSAGE, metadata);

        // Then
        assertNotNull(result);
        assertEquals(TENANT_ID, result.getTenantId());
        assertEquals(KnowledgeObjectType.TURN, result.getType());
        assertEquals(SESSION_ID, result.getSessionId());
        assertEquals(USER_ID, result.getUserId());
        assertNull(result.getParentId());
        assertEquals("[]", result.getTags());
        assertTrue(result.getMetadata().contains("source"));
        assertTrue(result.getMetadata().contains("model"));
        assertFalse(result.isArchived());
        assertNotNull(result.getCreatedAt());
        assertEquals(10, result.getOriginalTokens());

        verify(tokenCountingService).countTokens(ASSISTANT_MESSAGE, "gpt-3.5-turbo");
    }

    @Test
    void testCreateExtractedFacts_WithFacts() {
        // Given
        Fact fact = new Fact("User asked about system status", "conversation", 0.9);
        fact.setTags(Arrays.asList("important", "user-query"));
        MemoryExtraction memoryExtraction = new MemoryExtraction(
            Arrays.asList(fact), null, null, 0.9, Map.of("method", "llm")
        );
        when(tokenCountingService.countTokens(fact.getContent(), "gpt-3.5-turbo")).thenReturn(10);

        // When
        List<KnowledgeObject> results = knowledgeObjectService.createExtractedFacts(TENANT_ID, SESSION_ID, USER_ID, memoryExtraction);

        // Then
        assertEquals(1, results.size());
        KnowledgeObject result = results.get(0);
        assertEquals(TENANT_ID, result.getTenantId());
        assertEquals(KnowledgeObjectType.EXTRACTED_FACT, result.getType());
        assertEquals(SESSION_ID, result.getSessionId());
        assertEquals(USER_ID, result.getUserId());
        assertTrue(result.getMetadata().contains("source"));
        assertTrue(result.getMetadata().contains("confidence"));
        assertTrue(result.getMetadata().contains("extraction_method"));
        assertEquals(10, result.getOriginalTokens());

        verify(tokenCountingService).countTokens(fact.getContent(), "gpt-3.5-turbo");
    }

    @Test
    void testCreateExtractedFacts_WithEntities() {
        // Given
        Entity entity = new Entity("system", "infrastructure", "IT system infrastructure", 0.8);
        entity.setAttributes(Map.of("location", "remote", "priority", "high"));
        MemoryExtraction memoryExtraction = new MemoryExtraction(
            null, Arrays.asList(entity), null, 0.8, Map.of("method", "llm")
        );
        when(tokenCountingService.countTokens(entity.getName(), "gpt-3.5-turbo")).thenReturn(10);

        // When
        List<KnowledgeObject> results = knowledgeObjectService.createExtractedFacts(TENANT_ID, SESSION_ID, USER_ID, memoryExtraction);

        // Then
        assertEquals(1, results.size());
        KnowledgeObject result = results.get(0);
        assertEquals(KnowledgeObjectType.EXTRACTED_FACT, result.getType());
        assertTrue(result.getMetadata().contains("entity_type"));
        assertTrue(result.getMetadata().contains("description"));
        assertTrue(result.getMetadata().contains("confidence"));
        assertTrue(result.getMetadata().contains("attributes"));

        verify(tokenCountingService).countTokens(entity.getName(), "gpt-3.5-turbo");
    }

    @Test
    void testCreateExtractedFacts_WithTasks() {
        // Given
        Task task = new Task("Check system status", "pending");
        task.setPriority("medium");
        MemoryExtraction memoryExtraction = new MemoryExtraction(
            null, null, Arrays.asList(task), 0.7, Map.of("method", "llm")
        );
        when(tokenCountingService.countTokens(task.getDescription(), "gpt-3.5-turbo")).thenReturn(10);

        // When
        List<KnowledgeObject> results = knowledgeObjectService.createExtractedFacts(TENANT_ID, SESSION_ID, USER_ID, memoryExtraction);

        // Then
        assertEquals(1, results.size());
        KnowledgeObject result = results.get(0);
        assertEquals(KnowledgeObjectType.EXTRACTED_FACT, result.getType());
        assertTrue(result.getMetadata().contains("status"));
        assertTrue(result.getMetadata().contains("priority"));

        verify(tokenCountingService).countTokens(task.getDescription(), "gpt-3.5-turbo");
    }

    @Test
    void testCreateExtractedFacts_WithNullValues() {
        // Given
        MemoryExtraction memoryExtraction = new MemoryExtraction(null, null, null, 0.5, Map.of("method", "llm"));

        // When
        List<KnowledgeObject> results = knowledgeObjectService.createExtractedFacts(TENANT_ID, SESSION_ID, USER_ID, memoryExtraction);

        // Then
        assertEquals(0, results.size());
    }

    @Test
    void testCreateSessionMemory() {
        // Given
        String summary = "User inquired about system status and received confirmation";
        Map<String, Object> metadata = Map.of("turn_count", 2, "cumulative_tokens", 150);
        when(tokenCountingService.countTokens(summary, "gpt-3.5-turbo")).thenReturn(10);

        // When
        KnowledgeObject result = knowledgeObjectService.createSessionMemory(TENANT_ID, SESSION_ID, USER_ID, summary, metadata);

        // Then
        assertNotNull(result);
        assertEquals(TENANT_ID, result.getTenantId());
        assertEquals(KnowledgeObjectType.SESSION_MEMORY, result.getType());
        assertEquals(SESSION_ID, result.getSessionId());
        assertEquals(USER_ID, result.getUserId());
        assertTrue(result.getMetadata().contains("turn_count"));
        assertTrue(result.getMetadata().contains("cumulative_tokens"));
        assertEquals(10, result.getOriginalTokens());

        verify(tokenCountingService).countTokens(summary, "gpt-3.5-turbo");
    }

    @Test
    void testCreateContentVariants_WithAllVariants() {
        // Given
        KnowledgeObject knowledgeObject = new KnowledgeObject();
        knowledgeObject.setId("obj-123");
        String rawContent = "This is the raw content";
        String shortContent = "Short content";
        String bulletFacts = "• Fact 1\n• Fact 2";
        when(tokenCountingService.countTokens(rawContent, "gpt-3.5-turbo")).thenReturn(10);
        when(tokenCountingService.countTokens(shortContent, "gpt-3.5-turbo")).thenReturn(8);
        when(tokenCountingService.countTokens(bulletFacts, "gpt-3.5-turbo")).thenReturn(12);

        // When
        List<ContentVariant> results = knowledgeObjectService.createContentVariants(knowledgeObject, rawContent, shortContent, bulletFacts);

        // Then
        assertEquals(3, results.size());
        
        ContentVariant rawVariant = results.stream()
            .filter(v -> v.getVariant() == ContentVariantType.RAW)
            .findFirst().orElse(null);
        assertNotNull(rawVariant);
        assertEquals("obj-123", rawVariant.getKnowledgeObjectId());
        assertEquals(rawContent, rawVariant.getContent());
        assertEquals(10, rawVariant.getTokens());

        ContentVariant shortVariant = results.stream()
            .filter(v -> v.getVariant() == ContentVariantType.SHORT)
            .findFirst().orElse(null);
        assertNotNull(shortVariant);
        assertEquals(shortContent, shortVariant.getContent());

        ContentVariant bulletVariant = results.stream()
            .filter(v -> v.getVariant() == ContentVariantType.BULLET_FACTS)
            .findFirst().orElse(null);
        assertNotNull(bulletVariant);
        assertEquals(bulletFacts, bulletVariant.getContent());

        verify(tokenCountingService, times(3)).countTokens(anyString(), eq("gpt-3.5-turbo"));
    }

    @Test
    void testCreateContentVariants_WithNullValues() {
        // Given
        KnowledgeObject knowledgeObject = new KnowledgeObject();
        knowledgeObject.setId("obj-123");

        // When
        List<ContentVariant> results = knowledgeObjectService.createContentVariants(knowledgeObject, null, null, null);

        // Then
        assertEquals(0, results.size());
    }

    @Test
    void testCreateContentVariants_WithEmptyValues() {
        // Given
        KnowledgeObject knowledgeObject = new KnowledgeObject();
        knowledgeObject.setId("obj-123");

        // When
        List<ContentVariant> results = knowledgeObjectService.createContentVariants(knowledgeObject, "", "   ", null);

        // Then
        assertEquals(0, results.size());
    }

    @Test
    void testStoreKnowledgeObject() {
        // Given
        KnowledgeObject knowledgeObject = new KnowledgeObject();
        knowledgeObject.setId("obj-123");
        knowledgeObject.setTenantId(TENANT_ID);
        knowledgeObject.setType(KnowledgeObjectType.TURN);

        ContentVariant variant1 = new ContentVariant();
        variant1.setVariant(ContentVariantType.RAW);
        variant1.setContent("Content 1");

        ContentVariant variant2 = new ContentVariant();
        variant2.setVariant(ContentVariantType.SHORT);
        variant2.setContent("Content 2");

        List<ContentVariant> contentVariants = Arrays.asList(variant1, variant2);

        when(knowledgeObjectRepository.save(any(KnowledgeObject.class))).thenReturn(knowledgeObject);
        when(contentVariantRepository.save(any(ContentVariant.class))).thenReturn(new ContentVariant());

        // When
        KnowledgeObject result = knowledgeObjectService.storeKnowledgeObject(knowledgeObject, contentVariants);

        // Then
        assertNotNull(result);
        assertEquals("obj-123", result.getId());
        
        verify(knowledgeObjectRepository).save(knowledgeObject);
        verify(contentVariantRepository, times(2)).save(any(ContentVariant.class));
        
        // Verify that content variant IDs are set
        assertTrue(contentVariants.stream().allMatch(v -> "obj-123".equals(v.getKnowledgeObjectId())));
    }

    @Test
    void testFindBySessionAndType() {
        // Given
        List<KnowledgeObject> expectedObjects = Arrays.asList(
            new KnowledgeObject(), new KnowledgeObject()
        );
        when(knowledgeObjectRepository.findBySessionIdAndTenantIdAndType(SESSION_ID, TENANT_ID, KnowledgeObjectType.TURN))
            .thenReturn(expectedObjects);

        // When
        List<KnowledgeObject> results = knowledgeObjectService.findBySessionAndType(TENANT_ID, SESSION_ID, KnowledgeObjectType.TURN);

        // Then
        assertEquals(2, results.size());
        verify(knowledgeObjectRepository).findBySessionIdAndTenantIdAndType(SESSION_ID, TENANT_ID, KnowledgeObjectType.TURN);
    }

    @Test
    void testUpdateMetadata() {
        // Given
        String objectId = "obj-123";
        Map<String, Object> newMetadata = Map.of("updated", true, "timestamp", "2024-01-02");
        
        KnowledgeObject existingObject = new KnowledgeObject();
        existingObject.setId(objectId);
        existingObject.setTenantId(TENANT_ID);
        
        when(knowledgeObjectRepository.findByIdAndTenantId(objectId, TENANT_ID))
            .thenReturn(Optional.of(existingObject));
        when(knowledgeObjectRepository.save(any(KnowledgeObject.class)))
            .thenReturn(existingObject);

        // When
        KnowledgeObject result = knowledgeObjectService.updateMetadata(objectId, TENANT_ID, newMetadata);

        // Then
        assertNotNull(result);
        verify(knowledgeObjectRepository).findByIdAndTenantId(objectId, TENANT_ID);
        verify(knowledgeObjectRepository).save(existingObject);
    }

    @Test
    void testUpdateMetadata_ObjectNotFound() {
        // Given
        String objectId = "obj-123";
        Map<String, Object> newMetadata = Map.of("updated", true);
        
        when(knowledgeObjectRepository.findByIdAndTenantId(objectId, TENANT_ID))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            knowledgeObjectService.updateMetadata(objectId, TENANT_ID, newMetadata));
        
        verify(knowledgeObjectRepository).findByIdAndTenantId(objectId, TENANT_ID);
        verify(knowledgeObjectRepository, never()).save(any());
    }

    @Test
    void testArchiveKnowledgeObject() {
        // Given
        String objectId = "obj-123";
        KnowledgeObject existingObject = new KnowledgeObject();
        existingObject.setId(objectId);
        existingObject.setTenantId(TENANT_ID);
        existingObject.setArchived(false);
        
        when(knowledgeObjectRepository.findByIdAndTenantId(objectId, TENANT_ID))
            .thenReturn(Optional.of(existingObject));
        when(knowledgeObjectRepository.save(any(KnowledgeObject.class)))
            .thenReturn(existingObject);

        // When
        boolean result = knowledgeObjectService.archiveKnowledgeObject(objectId, TENANT_ID);

        // Then
        assertTrue(result);
        assertTrue(existingObject.isArchived());
        verify(knowledgeObjectRepository).findByIdAndTenantId(objectId, TENANT_ID);
        verify(knowledgeObjectRepository).save(existingObject);
    }

    @Test
    void testArchiveKnowledgeObject_ObjectNotFound() {
        // Given
        String objectId = "obj-123";
        
        when(knowledgeObjectRepository.findByIdAndTenantId(objectId, TENANT_ID))
            .thenReturn(Optional.empty());

        // When
        boolean result = knowledgeObjectService.archiveKnowledgeObject(objectId, TENANT_ID);

        // Then
        assertFalse(result);
        verify(knowledgeObjectRepository).findByIdAndTenantId(objectId, TENANT_ID);
        verify(knowledgeObjectRepository, never()).save(any());
    }
}

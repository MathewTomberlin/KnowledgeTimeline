package middleware.service.impl;

import middleware.model.ContentVariant;
import middleware.model.KnowledgeObject;
import middleware.repository.ContentVariantRepository;
import middleware.repository.KnowledgeObjectRepository;
import middleware.service.ContextBuilderService;
import middleware.service.VectorStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for RealContextBuilderService implementation.
 */
@ExtendWith(MockitoExtension.class)
class RealContextBuilderServiceTest {

    @Mock
    private VectorStoreService vectorStoreService;

    @Mock
    private KnowledgeObjectRepository knowledgeObjectRepository;

    @Mock
    private ContentVariantRepository contentVariantRepository;

    @InjectMocks
    private RealContextBuilderService contextBuilderService;

    @Test
    void testBuildContext_WithValidInput() {
        // Given
        String tenantId = "tenant1";
        String sessionId = "session1";
        String prompt = "What is machine learning?";
        Map<String, Object> knowledgeContext = new HashMap<>();
        knowledgeContext.put("diversity", 0.3);

        // Mock vector search results
        VectorStoreService.SimilarityMatch match1 = new VectorStoreService.SimilarityMatch("obj1", "var1", 0.9, "content1", new HashMap<>());
        VectorStoreService.SimilarityMatch match2 = new VectorStoreService.SimilarityMatch("obj2", "var2", 0.8, "content2", new HashMap<>());
        List<VectorStoreService.SimilarityMatch> matches = Arrays.asList(match1, match2);

        when(vectorStoreService.findSimilar(anyString(), eq(20), isNull(), eq(true), eq(0.3)))
            .thenReturn(matches);

        // Mock knowledge objects
        KnowledgeObject obj1 = createKnowledgeObject("obj1", "TURN", tenantId);
        KnowledgeObject obj2 = createKnowledgeObject("obj2", "SUMMARY", tenantId);

        when(knowledgeObjectRepository.findById("obj1")).thenReturn(Optional.of(obj1));
        when(knowledgeObjectRepository.findById("obj2")).thenReturn(Optional.of(obj2));

        // Mock content variants
        ContentVariant variant1 = createContentVariant("var1", "obj1", "SHORT", "ML is a subset of AI");
        ContentVariant variant2 = createContentVariant("var2", "obj2", "SHORT", "Machine learning algorithms");

        when(contentVariantRepository.findByKnowledgeObjectId("obj1")).thenReturn(Arrays.asList(variant1));
        when(contentVariantRepository.findByKnowledgeObjectId("obj2")).thenReturn(Arrays.asList(variant2));

        // When
        String result = contextBuilderService.buildContext(tenantId, sessionId, prompt, knowledgeContext);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertNotEquals("No relevant knowledge found.", result);
        assertNotEquals("Error retrieving knowledge context.", result);
        
        verify(vectorStoreService).findSimilar(anyString(), eq(20), isNull(), eq(true), eq(0.3));
        verify(knowledgeObjectRepository, times(2)).findById(anyString());
    }

    @Test
    void testBuildContext_WithNoVectorMatches() {
        // Given
        String tenantId = "tenant1";
        String sessionId = "session1";
        String prompt = "What is machine learning?";
        Map<String, Object> knowledgeContext = new HashMap<>();

        // Mock empty vector search results
        when(vectorStoreService.findSimilar(anyString(), eq(20), isNull(), eq(true), eq(0.3)))
            .thenReturn(Arrays.asList());

        // When
        String result = contextBuilderService.buildContext(tenantId, sessionId, prompt, knowledgeContext);

        // Then
        assertNotNull(result);
        assertEquals("No relevant knowledge found.", result);
        
        verify(vectorStoreService).findSimilar(anyString(), eq(20), isNull(), eq(true), eq(0.3));
        verify(knowledgeObjectRepository, never()).findById(anyString());
    }

    @Test
    void testGetRelevantKnowledge() {
        // Given
        String tenantId = "tenant1";
        String query = "machine learning";
        int maxResults = 10;
        Map<String, Object> filters = new HashMap<>();

        // Mock vector search results
        VectorStoreService.SimilarityMatch match1 = new VectorStoreService.SimilarityMatch("obj1", "var1", 0.9, "content1", new HashMap<>());
        List<VectorStoreService.SimilarityMatch> matches = Arrays.asList(match1);

        when(vectorStoreService.findSimilar(query, maxResults, filters, true, 0.3))
            .thenReturn(matches);

        // Mock knowledge object
        KnowledgeObject obj1 = createKnowledgeObject("obj1", "TURN", tenantId);
        when(knowledgeObjectRepository.findById("obj1")).thenReturn(Optional.of(obj1));

        // When
        List<ContextBuilderService.KnowledgeObject> result = contextBuilderService.getRelevantKnowledge(tenantId, query, maxResults, filters);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("obj1", result.get(0).getId());
        
        verify(vectorStoreService).findSimilar(query, maxResults, filters, true, 0.3);
        verify(knowledgeObjectRepository).findById("obj1");
    }

    @Test
    void testGetTokenBudget() {
        // Given
        String tenantId = "tenant1";

        // When
        int result = contextBuilderService.getTokenBudget(tenantId);

        // Then
        assertEquals(2000, result); // Default token budget
    }

    private KnowledgeObject createKnowledgeObject(String id, String type, String tenantId) {
        KnowledgeObject obj = new KnowledgeObject();
        obj.setId(id);
        obj.setType(middleware.model.KnowledgeObjectType.valueOf(type));
        obj.setTenantId(tenantId);
        obj.setCreatedAt(LocalDateTime.now());
        return obj;
    }

    private ContentVariant createContentVariant(String id, String knowledgeObjectId, String variant, String content) {
        ContentVariant variantObj = new ContentVariant();
        variantObj.setId(UUID.randomUUID());
        variantObj.setKnowledgeObjectId(knowledgeObjectId);
        variantObj.setVariant(middleware.model.ContentVariantType.valueOf(variant));
        variantObj.setContent(content);
        variantObj.setTokens(50);
        variantObj.setCreatedAt(LocalDateTime.now());
        return variantObj;
    }
}

package middleware.service.impl;

import middleware.service.VectorStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MockVectorStoreService.
 */
class MockVectorStoreServiceTest {

    private MockVectorStoreService vectorStoreService;

    @BeforeEach
    void setUp() {
        vectorStoreService = new MockVectorStoreService();
    }

    @Test
    void testStoreEmbedding_Success() {
        // Given
        String objectId = "obj-123";
        String variantId = "var-456";
        String text = "Test text content";
        List<Float> embedding = Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f);

        // When
        String result = vectorStoreService.storeEmbedding(objectId, variantId, text, embedding);

        // Then
        assertNotNull(result);
        // Mock implementation generates UUID format
        assertTrue(result.length() > 0);
    }

    @Test
    void testStoreEmbedding_WithNullObjectId() {
        // Given
        String objectId = null;
        String variantId = "var-456";
        String text = "Test text content";
        List<Float> embedding = Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f);

        // When & Then
        assertThrows(NullPointerException.class, () -> 
            vectorStoreService.storeEmbedding(objectId, variantId, text, embedding));
    }

    @Test
    void testStoreEmbedding_WithNullVariantId() {
        // Given
        String objectId = "obj-123";
        String variantId = null;
        String text = "Test text content";
        List<Float> embedding = Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f);

        // When & Then
        assertThrows(NullPointerException.class, () -> 
            vectorStoreService.storeEmbedding(objectId, variantId, text, embedding));
    }

    @Test
    void testStoreEmbedding_WithNullText() {
        // Given
        String objectId = "obj-123";
        String variantId = "var-456";
        String text = null;
        List<Float> embedding = Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f);

        // When & Then
        assertThrows(NullPointerException.class, () -> 
            vectorStoreService.storeEmbedding(objectId, variantId, text, embedding));
    }

    @Test
    void testStoreEmbedding_WithNullEmbedding() {
        // Given
        String objectId = "obj-123";
        String variantId = "var-456";
        String text = "Test text content";
        List<Float> embedding = null;

        // When & Then
        assertThrows(NullPointerException.class, () -> 
            vectorStoreService.storeEmbedding(objectId, variantId, text, embedding));
    }

    @Test
    void testStoreEmbedding_WithEmptyEmbedding() {
        // Given
        String objectId = "obj-123";
        String variantId = "var-456";
        String text = "Test text content";
        List<Float> embedding = Arrays.asList();

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            vectorStoreService.storeEmbedding(objectId, variantId, text, embedding));
    }

    @Test
    void testStoreEmbedding_WithEmptyText() {
        // Given
        String objectId = "obj-123";
        String variantId = "var-456";
        String text = "";
        List<Float> embedding = Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            vectorStoreService.storeEmbedding(objectId, variantId, text, embedding));
    }

    @Test
    void testStoreEmbedding_DuplicateVariantId() {
        // Given
        String objectId = "obj-123";
        String variantId = "var-456";
        String text1 = "First text content";
        String text2 = "Second text content";
        List<Float> embedding = Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f);

        // When
        String result1 = vectorStoreService.storeEmbedding(objectId, variantId, text1, embedding);
        String result2 = vectorStoreService.storeEmbedding(objectId, variantId, text2, embedding);

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        // Should return different embedding IDs for the same variant
        assertNotEquals(result1, result2);
    }

    @Test
    void testFindSimilar_Success() {
        // Given
        String objectId = "obj-123";
        String variantId = "var-456";
        String text = "Test text content";
        List<Float> embedding = Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f);
        
        vectorStoreService.storeEmbedding(objectId, variantId, text, embedding);

        // When
        List<VectorStoreService.SimilarityMatch> result = 
            vectorStoreService.findSimilar(text, 5, null, true, 0.3);

        // Then
        assertNotNull(result);
        // Mock implementation always returns results due to random similarity calculation
        assertTrue(result.size() > 0);
        assertEquals(objectId, result.get(0).getObjectId());
        assertEquals(variantId, result.get(0).getVariantId());
        assertEquals(text, result.get(0).getContent());
    }

    @Test
    void testFindSimilar_WithFilters() {
        // Given
        String objectId = "obj-123";
        String variantId = "var-456";
        String text = "Test text content";
        List<Float> embedding = Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f);
        
        vectorStoreService.storeEmbedding(objectId, variantId, text, embedding);
        
        Map<String, Object> filters = new HashMap<>();
        filters.put("tenantId", "tenant-1");

        // When
        List<VectorStoreService.SimilarityMatch> result = 
            vectorStoreService.findSimilar(text, 5, filters, true, 0.3);

        // Then
        assertNotNull(result);
        // Mock implementation always passes filters and returns results
        assertTrue(result.size() > 0);
    }

    @Test
    void testFindSimilar_WithMMR() {
        // Given
        String objectId = "obj-123";
        String variantId = "var-456";
        String text = "Test text content";
        List<Float> embedding = Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f);
        
        vectorStoreService.storeEmbedding(objectId, variantId, text, embedding);

        // When
        List<VectorStoreService.SimilarityMatch> result = 
            vectorStoreService.findSimilar(text, 5, null, true, 0.5);

        // Then
        assertNotNull(result);
        // Mock implementation always returns results
        assertTrue(result.size() > 0);
    }

    @Test
    void testFindSimilar_WithoutMMR() {
        // Given
        String objectId = "obj-123";
        String variantId = "var-456";
        String text = "Test text content";
        List<Float> embedding = Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f);
        
        vectorStoreService.storeEmbedding(objectId, variantId, text, embedding);

        // When
        List<VectorStoreService.SimilarityMatch> result = 
            vectorStoreService.findSimilar(text, 5, null, false, 0.3);

        // Then
        assertNotNull(result);
        // Mock implementation always returns results
        assertTrue(result.size() > 0);
    }

    @Test
    void testFindSimilar_WithLimit() {
        // Given
        String objectId = "obj-123";
        String variantId = "var-456";
        String text = "Test text content";
        List<Float> embedding = Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f);
        
        vectorStoreService.storeEmbedding(objectId, variantId, text, embedding);

        // When
        List<VectorStoreService.SimilarityMatch> result = 
            vectorStoreService.findSimilar(text, 1, null, true, 0.3);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testFindSimilar_WithZeroLimit() {
        // Given
        String objectId = "obj-123";
        String variantId = "var-456";
        String text = "Test text content";
        List<Float> embedding = Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f);
        
        vectorStoreService.storeEmbedding(objectId, variantId, text, embedding);

        // When
        List<VectorStoreService.SimilarityMatch> result = 
            vectorStoreService.findSimilar(text, 0, null, true, 0.3);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindSimilar_WithNegativeLimit() {
        // Given
        String objectId = "obj-123";
        String variantId = "var-456";
        String text = "Test text content";
        List<Float> embedding = Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f);
        
        vectorStoreService.storeEmbedding(objectId, variantId, text, embedding);

        // When
        List<VectorStoreService.SimilarityMatch> result = 
            vectorStoreService.findSimilar(text, -1, null, true, 0.3);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindSimilar_WithEmptyQuery() {
        // Given
        String objectId = "obj-123";
        String variantId = "var-456";
        String text = "Test text content";
        List<Float> embedding = Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f);
        
        vectorStoreService.storeEmbedding(objectId, variantId, text, embedding);

        // When
        List<VectorStoreService.SimilarityMatch> result = 
            vectorStoreService.findSimilar("", 5, null, true, 0.3);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindSimilar_WithNullQuery() {
        // Given
        String objectId = "obj-123";
        String variantId = "var-456";
        String text = "Test text content";
        List<Float> embedding = Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f);
        
        vectorStoreService.storeEmbedding(objectId, variantId, text, embedding);

        // When
        List<VectorStoreService.SimilarityMatch> result = 
            vectorStoreService.findSimilar(null, 5, null, true, 0.3);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testDeleteEmbedding_Success() {
        // Given
        String objectId = "obj-123";
        String variantId = "var-456";
        String text = "Test text content";
        List<Float> embedding = Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f);
        
        String embeddingId = vectorStoreService.storeEmbedding(objectId, variantId, text, embedding);

        // When
        boolean result = vectorStoreService.deleteEmbedding(embeddingId);

        // Then
        assertTrue(result);
    }

    @Test
    void testDeleteEmbedding_NotFound() {
        // Given
        String nonExistentEmbeddingId = "emb-nonexistent";

        // When
        boolean result = vectorStoreService.deleteEmbedding(nonExistentEmbeddingId);

        // Then
        assertFalse(result);
    }

    @Test
    void testDeleteEmbedding_WithNullId() {
        // Given
        String embeddingId = null;

        // When & Then
        assertThrows(NullPointerException.class, () -> 
            vectorStoreService.deleteEmbedding(embeddingId));
    }

    @Test
    void testDeleteEmbedding_WithEmptyId() {
        // Given
        String embeddingId = "";

        // When & Then
        // Mock implementation doesn't validate empty ID, so this should return false
        boolean result = vectorStoreService.deleteEmbedding(embeddingId);
        assertFalse(result);
    }

    @Test
    void testIsHealthy() {
        // When
        boolean result = vectorStoreService.isHealthy();

        // Then
        assertTrue(result); // Mock service is always healthy
    }

    @Test
    void testGetEmbeddingDimension() {
        // When
        int result = vectorStoreService.getEmbeddingDimension();

        // Then
        assertEquals(384, result); // Mock dimension
    }

    @Test
    void testFindSimilar_DefaultMethod() {
        // Given
        String objectId = "obj-123";
        String variantId = "var-456";
        String text = "Test text content";
        List<Float> embedding = Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f);
        
        vectorStoreService.storeEmbedding(objectId, variantId, text, embedding);

        // When
        List<VectorStoreService.SimilarityMatch> result = 
            vectorStoreService.findSimilar(text, 5, null);

        // Then
        assertNotNull(result);
        // Mock implementation always returns results
        assertTrue(result.size() > 0);
        // Should use default MMR=true and diversity=0.3
    }

    @Test
    void testFindSimilar_WithHighDiversity() {
        // Given
        String objectId = "obj-123";
        String variantId = "var-456";
        String text = "Test text content";
        List<Float> embedding = Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f);
        
        vectorStoreService.storeEmbedding(objectId, variantId, text, embedding);

        // When
        List<VectorStoreService.SimilarityMatch> result = 
            vectorStoreService.findSimilar(text, 5, null, true, 0.9);

        // Then
        assertNotNull(result);
        // Mock implementation always returns results
        assertTrue(result.size() > 0);
    }

    @Test
    void testFindSimilar_WithLowDiversity() {
        // Given
        String objectId = "obj-123";
        String variantId = "var-456";
        String text = "Test text content";
        List<Float> embedding = Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f);
        
        vectorStoreService.storeEmbedding(objectId, variantId, text, embedding);

        // When
        List<VectorStoreService.SimilarityMatch> result = 
            vectorStoreService.findSimilar(text, 5, null, true, 0.1);

        // Then
        assertNotNull(result);
        // Mock implementation always returns results
        assertTrue(result.size() > 0);
    }

    @Test
    void testFindSimilar_WithZeroDiversity() {
        // Given
        String objectId = "obj-123";
        String variantId = "var-456";
        String text = "Test text content";
        List<Float> embedding = Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f);
        
        vectorStoreService.storeEmbedding(objectId, variantId, text, embedding);

        // When
        List<VectorStoreService.SimilarityMatch> result = 
            vectorStoreService.findSimilar(text, 5, null, true, 0.0);

        // Then
        assertNotNull(result);
        // Mock implementation always returns results
        assertTrue(result.size() > 0);
    }

    @Test
    void testFindSimilar_WithOneDiversity() {
        // Given
        String objectId = "obj-123";
        String variantId = "var-456";
        String text = "Test text content";
        List<Float> embedding = Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f);
        
        vectorStoreService.storeEmbedding(objectId, variantId, text, embedding);

        // When
        List<VectorStoreService.SimilarityMatch> result = 
            vectorStoreService.findSimilar(text, 5, null, true, 1.0);

        // Then
        assertNotNull(result);
        // Mock implementation always returns results
        assertTrue(result.size() > 0);
    }
}

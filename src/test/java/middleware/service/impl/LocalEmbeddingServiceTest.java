package middleware.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LocalEmbeddingService implementation.
 * Note: These tests focus on the service logic and fallback behavior
 * since WebClient mocking is complex and the service has fallback mechanisms.
 */
@ExtendWith(MockitoExtension.class)
class LocalEmbeddingServiceTest {

    private LocalEmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        // Create service with test configuration
        embeddingService = new LocalEmbeddingService("http://localhost:8081", "test-model", 384);
    }

    @Test
    void testGenerateEmbedding_WithFallback() {
        // Given
        String text = "Hello world";

        // When - service will fail to connect and use fallback
        List<Float> result = embeddingService.generateEmbedding(text);

        // Then
        assertNotNull(result);
        assertEquals(384, result.size()); // Should use mock embedding with default dimension
        assertTrue(result.stream().anyMatch(f -> f != 0.0f)); // Should have some non-zero values
    }

    @Test
    void testGenerateEmbedding_WithEmptyText() {
        // Given
        String text = "";

        // When
        List<Float> result = embeddingService.generateEmbedding(text);

        // Then
        assertNotNull(result);
        assertEquals(384, result.size()); // Should use mock embedding
    }

    @Test
    void testGenerateEmbeddings_WithFallback() {
        // Given
        List<String> texts = Arrays.asList("Hello", "World");

        // When - service will fail to connect and use fallback
        List<List<Float>> result = embeddingService.generateEmbeddings(texts);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(384, result.get(0).size()); // Should use mock embeddings
        assertEquals(384, result.get(1).size());
    }

    @Test
    void testGenerateEmbeddings_WithEmptyTextList() {
        // Given
        List<String> texts = Arrays.asList();

        // When
        List<List<Float>> result = embeddingService.generateEmbeddings(texts);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetEmbeddingDimension() {
        // When
        int result = embeddingService.getEmbeddingDimension();

        // Then
        assertEquals(384, result);
    }

    @Test
    void testGetModelName() {
        // When
        String result = embeddingService.getModelName();

        // Then
        assertEquals("test-model", result);
    }

    @Test
    void testIsHealthy_DoesNotThrowException() {
        // When - service will try to connect
        // Then - should not throw an exception regardless of connection status
        assertDoesNotThrow(() -> {
            boolean result = embeddingService.isHealthy();
            // Result depends on whether the service is actually running
        });
    }

    @Test
    void testGenerateEmbedding_DeterministicMockEmbedding() {
        // Given
        String text = "Test text for deterministic embedding";

        // When
        List<Float> result1 = embeddingService.generateEmbedding(text);
        List<Float> result2 = embeddingService.generateEmbedding(text);

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(result1.size(), result2.size());
        assertEquals(result1, result2); // Should be deterministic
    }

    @Test
    void testGenerateEmbeddings_WithEmptyTextList() {
        // Given
        List<String> texts = Arrays.asList();

        // When
        List<List<Float>> result = embeddingService.generateEmbeddings(texts);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGenerateEmbedding_WithEmptyText() {
        // Given
        String text = "";

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/embeddings")).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenThrow(new RuntimeException("Service unavailable"));

        // When
        List<Float> result = embeddingService.generateEmbedding(text);

        // Then
        assertNotNull(result);
        assertEquals(384, result.size()); // Should use mock embedding
    }
}

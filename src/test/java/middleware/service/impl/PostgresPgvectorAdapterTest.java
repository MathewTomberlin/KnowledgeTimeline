package middleware.service.impl;

import middleware.service.VectorStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for PostgresPgvectorAdapter implementation.
 */
@ExtendWith(MockitoExtension.class)
class PostgresPgvectorAdapterTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private PostgresPgvectorAdapter postgresPgvectorAdapter;

    @BeforeEach
    void setUp() {
        // Create the adapter with mocked DataSource and default embedding dimension
        postgresPgvectorAdapter = new PostgresPgvectorAdapter(dataSource, 384);
        
        // Use reflection to inject the mocked JdbcTemplate
        try {
            java.lang.reflect.Field jdbcTemplateField = PostgresPgvectorAdapter.class.getDeclaredField("jdbcTemplate");
            jdbcTemplateField.setAccessible(true);
            jdbcTemplateField.set(postgresPgvectorAdapter, jdbcTemplate);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mocked JdbcTemplate", e);
        }
    }

    @Test
    void testStoreEmbedding_WithValidInput() {
        // Given
        String objectId = "obj1";
        String variantId = "var1";
        String text = "Sample text for embedding";
        List<Float> embedding = Arrays.asList(0.1f, 0.2f, 0.3f, 0.4f, 0.5f);

        when(jdbcTemplate.update(anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(1);

        // When
        String result = postgresPgvectorAdapter.storeEmbedding(objectId, variantId, text, embedding);

        // Then
        assertNotNull(result);
        assertTrue(result.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")); // UUID format
        
        verify(jdbcTemplate).update(
            contains("INSERT INTO embeddings"),
            anyString(), // embeddingId
            eq(variantId),
            eq(text),
            contains("[0.1,0.2,0.3,0.4,0.5]") // vector format
        );
    }

    @Test
    void testStoreEmbedding_WithEmptyEmbedding() {
        // Given
        String objectId = "obj1";
        String variantId = "var1";
        String text = "Sample text";
        List<Float> embedding = Arrays.asList();

        when(jdbcTemplate.update(anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(1);

        // When
        String result = postgresPgvectorAdapter.storeEmbedding(objectId, variantId, text, embedding);

        // Then
        assertNotNull(result);
        assertTrue(result.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"));
        
        verify(jdbcTemplate).update(
            contains("INSERT INTO embeddings"),
            anyString(),
            eq(variantId),
            eq(text),
            eq("[]") // empty vector
        );
    }

    @Test
    void testStoreEmbedding_WithSingleValueEmbedding() {
        // Given
        String objectId = "obj1";
        String variantId = "var1";
        String text = "Sample text";
        List<Float> embedding = Arrays.asList(0.5f);

        when(jdbcTemplate.update(anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(1);

        // When
        String result = postgresPgvectorAdapter.storeEmbedding(objectId, variantId, text, embedding);

        // Then
        assertNotNull(result);
        
        verify(jdbcTemplate).update(
            contains("INSERT INTO embeddings"),
            anyString(),
            eq(variantId),
            eq(text),
            eq("[0.5]") // single value vector
        );
    }

    @Test
    void testFindSimilar_WithValidQuery() {
        // Given
        String queryText = "search query";
        int k = 5;
        Map<String, Object> filters = new HashMap<>();
        boolean withMMR = false;
        double diversity = 0.3;

        List<VectorStoreService.SimilarityMatch> mockResults = Arrays.asList(
            new VectorStoreService.SimilarityMatch("emb1", "var1", "text1", 0.8, new HashMap<>()),
            new VectorStoreService.SimilarityMatch("emb2", "var2", "text2", 0.9, new HashMap<>())
        );

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString(), anyInt()))
            .thenReturn(mockResults);

        // When
        List<VectorStoreService.SimilarityMatch> result = postgresPgvectorAdapter.findSimilar(
            queryText, k, filters, withMMR, diversity);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("emb1", result.get(0).getObjectId());
        assertEquals("emb2", result.get(1).getObjectId());
        
        verify(jdbcTemplate).query(
            contains("SELECT e.id, e.variant_id, e.text_snippet"),
            any(RowMapper.class),
            contains("[0.1,0.2,0.3,0.4,0.5]"), // mock embedding vector
            eq(k)
        );
    }

    @Test
    void testFindSimilar_WithEmptyResults() {
        // Given
        String queryText = "search query";
        int k = 5;
        Map<String, Object> filters = new HashMap<>();
        boolean withMMR = false;
        double diversity = 0.3;

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString(), anyInt()))
            .thenReturn(Arrays.asList());

        // When
        List<VectorStoreService.SimilarityMatch> result = postgresPgvectorAdapter.findSimilar(
            queryText, k, filters, withMMR, diversity);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFindSimilar_WithNullFilters() {
        // Given
        String queryText = "search query";
        int k = 5;
        Map<String, Object> filters = null;
        boolean withMMR = false;
        double diversity = 0.3;

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString(), anyInt()))
            .thenReturn(Arrays.asList());

        // When
        List<VectorStoreService.SimilarityMatch> result = postgresPgvectorAdapter.findSimilar(
            queryText, k, filters, withMMR, diversity);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testDeleteEmbedding_WithValidId() {
        // Given
        String embeddingId = "emb1";

        when(jdbcTemplate.update(anyString(), anyString())).thenReturn(1);

        // When
        postgresPgvectorAdapter.deleteEmbedding(embeddingId);

        // Then
        verify(jdbcTemplate).update("DELETE FROM embeddings WHERE id = ?", embeddingId);
    }

    @Test
    void testDeleteEmbedding_WithEmptyId() {
        // Given
        String embeddingId = "";

        when(jdbcTemplate.update(anyString(), anyString())).thenReturn(1);

        // When
        postgresPgvectorAdapter.deleteEmbedding(embeddingId);

        // Then
        verify(jdbcTemplate).update("DELETE FROM embeddings WHERE id = ?", embeddingId);
    }

    @Test
    void testIsHealthy_WhenDatabaseIsHealthy() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(0);

        // When
        boolean result = postgresPgvectorAdapter.isHealthy();

        // Then
        assertTrue(result);
        verify(jdbcTemplate).queryForObject("SELECT COUNT(*) FROM embeddings LIMIT 1", Integer.class);
    }

    @Test
    void testIsHealthy_WhenDatabaseThrowsException() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class)))
            .thenThrow(new RuntimeException("Database connection failed"));

        // When
        boolean result = postgresPgvectorAdapter.isHealthy();

        // Then
        assertFalse(result);
    }

    @Test
    void testGetStatistics_WhenDatabaseIsHealthy() {
        // Given
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM embeddings", Integer.class)).thenReturn(100);

        // When
        Map<String, Object> result = postgresPgvectorAdapter.getStatistics();

        // Then
        assertNotNull(result);
        assertEquals(100, result.get("total_embeddings"));
        assertEquals(384, result.get("embedding_dimension")); // default from application.yml
        assertEquals("PostgreSQL with pgvector", result.get("database_type"));
        
        verify(jdbcTemplate).queryForObject("SELECT COUNT(*) FROM embeddings", Integer.class);
    }

    @Test
    void testGetStatistics_WhenDatabaseThrowsException() {
        // Given
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM embeddings", Integer.class))
            .thenThrow(new RuntimeException("Database error"));

        // When
        Map<String, Object> result = postgresPgvectorAdapter.getStatistics();

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("error"));
        assertTrue(result.get("error").toString().contains("Failed to get statistics"));
        assertEquals("PostgreSQL with pgvector", result.get("database_type"));
    }

    @Test
    void testGetStatistics_WhenCountReturnsNull() {
        // Given
        when(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM embeddings", Integer.class)).thenReturn(null);

        // When
        Map<String, Object> result = postgresPgvectorAdapter.getStatistics();

        // Then
        assertNotNull(result);
        assertEquals(0, result.get("total_embeddings"));
        assertEquals(384, result.get("embedding_dimension"));
        assertEquals("PostgreSQL with pgvector", result.get("database_type"));
    }

    @Test
    void testConstructor_WithCustomEmbeddingDimension() {
        // Given
        int customDimension = 512;
        
        // When
        PostgresPgvectorAdapter adapter = new PostgresPgvectorAdapter(dataSource, customDimension);
        
        // Then
        assertNotNull(adapter);
        // Note: We can't easily test the private field, but the constructor should not throw
    }

    @Test
    void testFindSimilar_WithMMREnabled() {
        // Given
        String queryText = "search query";
        int k = 5;
        Map<String, Object> filters = new HashMap<>();
        boolean withMMR = true;
        double diversity = 0.3;

        // Note: The current implementation doesn't actually use MMR, but it should still work
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString(), anyInt()))
            .thenReturn(Arrays.asList());

        // When
        List<VectorStoreService.SimilarityMatch> result = postgresPgvectorAdapter.findSimilar(
            queryText, k, filters, withMMR, diversity);

        // Then
        assertNotNull(result);
        // The current implementation doesn't use MMR, so this is just a placeholder test
        // TODO: Implement actual MMR algorithm and test it properly
    }

    @Test
    void testFindSimilar_WithLargeK() {
        // Given
        String queryText = "search query";
        int k = 1000; // Large number
        Map<String, Object> filters = new HashMap<>();
        boolean withMMR = false;
        double diversity = 0.3;

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString(), anyInt()))
            .thenReturn(Arrays.asList());

        // When
        List<VectorStoreService.SimilarityMatch> result = postgresPgvectorAdapter.findSimilar(
            queryText, k, filters, withMMR, diversity);

        // Then
        assertNotNull(result);
        verify(jdbcTemplate).query(
            anyString(),
            any(RowMapper.class),
            anyString(),
            eq(1000)
        );
    }

    @Test
    void testFindSimilar_WithZeroK() {
        // Given
        String queryText = "search query";
        int k = 0;
        Map<String, Object> filters = new HashMap<>();
        boolean withMMR = false;
        double diversity = 0.3;

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString(), anyInt()))
            .thenReturn(Arrays.asList());

        // When
        List<VectorStoreService.SimilarityMatch> result = postgresPgvectorAdapter.findSimilar(
            queryText, k, filters, withMMR, diversity);

        // Then
        assertNotNull(result);
        verify(jdbcTemplate).query(
            anyString(),
            any(RowMapper.class),
            anyString(),
            eq(0)
        );
    }
}

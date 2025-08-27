package middleware.vector.impl;

import middleware.service.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PostgreSQL pgvector implementation of VectorStoreService.
 * Uses pgvector extension for vector similarity search.
 */
@Service
public class PostgresPgvectorAdapter implements VectorStoreService {
    
    private static final Logger logger = LoggerFactory.getLogger(PostgresPgvectorAdapter.class);
    
    private final JdbcTemplate jdbcTemplate;
    private final int embeddingDimension;
    
    @Value("${vector.store.embedding-dimension:384}")
    private int configEmbeddingDimension;
    
    public PostgresPgvectorAdapter(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.embeddingDimension = configEmbeddingDimension;
        initializeDatabase();
    }
    
    @Override
    public String storeEmbedding(String objectId, String variantId, String text, List<Float> embedding) {
        try {
            // Convert List<Float> to array for PostgreSQL
            float[] embeddingArray = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                embeddingArray[i] = embedding.get(i);
            }
            
            String sql = """
                INSERT INTO embeddings (id, variant_id, text_snippet, embedding, created_at)
                VALUES (?, ?, ?, ?, NOW())
                ON CONFLICT (variant_id) DO UPDATE SET
                    text_snippet = EXCLUDED.text_snippet,
                    embedding = EXCLUDED.embedding,
                    updated_at = NOW()
                RETURNING id
                """;
            
            String embeddingId = jdbcTemplate.queryForObject(sql, String.class,
                UUID.randomUUID().toString(), variantId, text, embeddingArray);
            
            logger.debug("Stored embedding for variant {} with ID {}", variantId, embeddingId);
            return embeddingId;
            
        } catch (Exception e) {
            logger.error("Failed to store embedding for variant {}", variantId, e);
            throw new RuntimeException("Failed to store embedding", e);
        }
    }
    
    @Override
    public List<SimilarityMatch> findSimilar(String queryText, int k, Map<String, Object> filters, 
                                           boolean withMMR, double diversity) {
        try {
            // For now, implement basic cosine similarity search
            // TODO: Implement MMR algorithm for diversity
            String sql = """
                SELECT e.id, e.variant_id, e.text_snippet, e.embedding,
                       cv.knowledge_object_id, cv.content, cv.metadata,
                       (e.embedding <=> ?) as similarity_score
                FROM embeddings e
                JOIN content_variants cv ON e.variant_id = cv.id
                JOIN knowledge_objects ko ON cv.knowledge_object_id = ko.id
                WHERE ko.archived = false
                """;
            
            List<Object> params = new ArrayList<>();
            params.add(convertTextToEmbedding(queryText)); // This would need an embedding service
            
            // Add filters
            if (filters != null) {
                if (filters.containsKey("tenant_id")) {
                    sql += " AND ko.tenant_id = ?";
                    params.add(filters.get("tenant_id"));
                }
                if (filters.containsKey("object_types")) {
                    @SuppressWarnings("unchecked")
                    List<String> types = (List<String>) filters.get("object_types");
                    if (!types.isEmpty()) {
                        String placeholders = types.stream().map(t -> "?").collect(Collectors.joining(","));
                        sql += " AND ko.type IN (" + placeholders + ")";
                        params.addAll(types);
                    }
                }
            }
            
            sql += " ORDER BY similarity_score ASC LIMIT ?";
            params.add(k);
            
            return jdbcTemplate.query(sql, params.toArray(), this::mapToSimilarityMatch);
            
        } catch (Exception e) {
            logger.error("Failed to find similar content for query: {}", queryText, e);
            throw new RuntimeException("Failed to find similar content", e);
        }
    }
    
    @Override
    public boolean deleteEmbedding(String embeddingId) {
        try {
            String sql = "DELETE FROM embeddings WHERE id = ?";
            int rowsAffected = jdbcTemplate.update(sql, embeddingId);
            logger.debug("Deleted embedding with ID: {}, rows affected: {}", embeddingId, rowsAffected);
            return rowsAffected > 0;
        } catch (Exception e) {
            logger.error("Failed to delete embedding with ID: {}", embeddingId, e);
            return false;
        }
    }
    
    @Override
    public boolean isHealthy() {
        try {
            // Check if pgvector extension is available
            String sql = "SELECT 1 FROM pg_extension WHERE extname = 'vector'";
            jdbcTemplate.queryForObject(sql, Integer.class);
            
            // Check if embeddings table exists and is accessible
            sql = "SELECT COUNT(*) FROM embeddings LIMIT 1";
            jdbcTemplate.queryForObject(sql, Integer.class);
            
            return true;
        } catch (Exception e) {
            logger.error("Health check failed for PostgresPgvectorAdapter", e);
            return false;
        }
    }
    
    @Override
    public int getEmbeddingDimension() {
        return embeddingDimension;
    }
    
    private void initializeDatabase() {
        try {
            // Create embeddings table if it doesn't exist
            String createTableSql = """
                CREATE TABLE IF NOT EXISTS embeddings (
                    id VARCHAR(255) PRIMARY KEY,
                    variant_id VARCHAR(255) NOT NULL UNIQUE,
                    text_snippet TEXT NOT NULL,
                    embedding vector(%d) NOT NULL,
                    created_at TIMESTAMP DEFAULT NOW(),
                    updated_at TIMESTAMP DEFAULT NOW()
                )
                """.formatted(embeddingDimension);
            
            jdbcTemplate.execute(createTableSql);
            
            // Create index for similarity search
            String createIndexSql = """
                CREATE INDEX IF NOT EXISTS embeddings_ivfflat 
                ON embeddings USING ivfflat (embedding) WITH (lists = 100)
                """;
            
            jdbcTemplate.execute(createIndexSql);
            
            logger.info("Initialized embeddings table with pgvector support");
            
        } catch (Exception e) {
            logger.error("Failed to initialize embeddings table", e);
            throw new RuntimeException("Failed to initialize vector store", e);
        }
    }
    
    private SimilarityMatch mapToSimilarityMatch(ResultSet rs, int rowNum) throws SQLException {
        String objectId = rs.getString("knowledge_object_id");
        String variantId = rs.getString("variant_id");
        double similarityScore = rs.getDouble("similarity_score");
        String content = rs.getString("content");
        
        // Parse metadata JSON if available
        Map<String, Object> metadata = new HashMap<>();
        try {
            String metadataJson = rs.getString("metadata");
            if (metadataJson != null && !metadataJson.isEmpty()) {
                // Simple JSON parsing - in production, use proper JSON library
                // For now, just add basic info
                metadata.put("variant_id", variantId);
                metadata.put("similarity_score", similarityScore);
            }
        } catch (Exception e) {
            logger.debug("Failed to parse metadata for variant {}", variantId, e);
        }
        
        return new SimilarityMatch(objectId, variantId, similarityScore, content, metadata);
    }
    
    private float[] convertTextToEmbedding(String text) {
        // TODO: This should use an embedding service
        // For now, return a mock embedding of the correct dimension
        float[] mockEmbedding = new float[embeddingDimension];
        Random random = new Random(text.hashCode()); // Deterministic for testing
        for (int i = 0; i < embeddingDimension; i++) {
            mockEmbedding[i] = random.nextFloat() * 2 - 1; // Range [-1, 1]
        }
        return mockEmbedding;
    }
}

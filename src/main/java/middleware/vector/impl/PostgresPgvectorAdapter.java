package middleware.vector.impl;

import middleware.service.VectorStoreService;
import middleware.service.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
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
@Profile({"local", "docker"})  // Only active for production profiles
public class PostgresPgvectorAdapter implements VectorStoreService {

    private static final Logger logger = LoggerFactory.getLogger(PostgresPgvectorAdapter.class);

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingService embeddingService;
    private final int embeddingDimension;

    public PostgresPgvectorAdapter(DataSource dataSource, EmbeddingService embeddingService,
                                 @Value("${vector.store.embedding-dimension:384}") int embeddingDimension) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.embeddingService = embeddingService;
        this.embeddingDimension = embeddingDimension;
        initializeDatabase();
    }
    
    @Override
    public String storeEmbedding(String objectId, String variantId, String text, List<Float> embedding) {
        try {
            // Convert List<Float> to vector string format for PostgreSQL
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < embedding.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(embedding.get(i));
            }
            sb.append("]");
            String vectorString = sb.toString();

            String sql = """
                INSERT INTO embeddings (id, variant_id, text_snippet, embedding, created_at)
                VALUES (?, ?, ?, ?::vector, NOW())
                ON CONFLICT (variant_id) DO UPDATE SET
                    text_snippet = EXCLUDED.text_snippet,
                    embedding = EXCLUDED.embedding,
                    updated_at = NOW()
                RETURNING id
                """;

            String embeddingId = jdbcTemplate.queryForObject(sql, String.class,
                UUID.randomUUID().toString(), variantId, text, vectorString);

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
            List<Object> params = new ArrayList<>();
            float[] queryEmbedding = convertTextToEmbedding(queryText);
            // Convert to vector string format
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < queryEmbedding.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(queryEmbedding[i]);
            }
            sb.append("]");
            String vectorString = sb.toString();

            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT e.id, e.variant_id, e.text_snippet, e.embedding, ");
            sqlBuilder.append("cv.knowledge_object_id, cv.content, ko.metadata, ");
            sqlBuilder.append("(e.embedding <=> '").append(vectorString).append("') as similarity_score ");
            sqlBuilder.append("FROM embeddings e ");
            sqlBuilder.append("JOIN content_variants cv ON e.variant_id = cv.id ");
            sqlBuilder.append("JOIN knowledge_objects ko ON cv.knowledge_object_id = ko.id ");
            sqlBuilder.append("WHERE ko.archived = false");

            String sql = sqlBuilder.toString();
            
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
            // Check if we can connect to the database
            String sql = "SELECT 1";
            jdbcTemplate.queryForObject(sql, Integer.class);

            // Check if pgvector extension is available
            try {
                sql = "SELECT 1 FROM pg_extension WHERE extname = 'vector'";
                jdbcTemplate.queryForObject(sql, Integer.class);
            } catch (Exception e) {
                logger.warn("pgvector extension not available", e);
                return false;
            }

            // Try to check if embeddings table exists and is accessible
            try {
                sql = "SELECT COUNT(*) FROM embeddings LIMIT 1";
                jdbcTemplate.queryForObject(sql, Integer.class);
            } catch (Exception e) {
                logger.warn("embeddings table not accessible, may need initialization", e);
                // Try to initialize and then check again
                try {
                    initializeDatabase();
                    sql = "SELECT COUNT(*) FROM embeddings LIMIT 1";
                    jdbcTemplate.queryForObject(sql, Integer.class);
                } catch (Exception initException) {
                    logger.error("Failed to initialize and access embeddings table", initException);
                    return false;
                }
            }

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
            // Check if pgvector extension exists
            try {
                jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
                logger.info("pgvector extension is available");
            } catch (Exception e) {
                logger.warn("pgvector extension not available, vector search will not work", e);
                return;
            }

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
                ON embeddings USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100)
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
        try {
            List<Float> embedding = embeddingService.generateEmbedding(text);
            float[] embeddingArray = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                embeddingArray[i] = embedding.get(i);
            }
            return embeddingArray;
        } catch (Exception e) {
            logger.error("Failed to generate embedding for text: {}", text, e);
            // Fallback to mock embedding if embedding service fails
            float[] mockEmbedding = new float[embeddingDimension];
            Random random = new Random(text.hashCode()); // Deterministic for testing
            for (int i = 0; i < embeddingDimension; i++) {
                mockEmbedding[i] = random.nextFloat() * 2 - 1; // Range [-1, 1]
            }
            return mockEmbedding;
        }
    }
}

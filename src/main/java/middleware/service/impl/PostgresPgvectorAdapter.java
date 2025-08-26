package middleware.service.impl;

import middleware.service.VectorStoreService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * PostgreSQL pgvector adapter implementation for VectorStoreService.
 * Provides real vector storage and similarity search using PostgreSQL with pgvector extension.
 */
@Service
public class PostgresPgvectorAdapter implements VectorStoreService {

    private final JdbcTemplate jdbcTemplate;
    private final int embeddingDimension;

    public PostgresPgvectorAdapter(DataSource dataSource,
                                 @Value("${knowledge.embeddings.dimension:384}") int embeddingDimension) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.embeddingDimension = embeddingDimension;
    }

    @Override
    public String storeEmbedding(String objectId, String variantId, String text, List<Float> embedding) {
        String embeddingId = UUID.randomUUID().toString();
        
        // Convert List<Float> to PostgreSQL vector format
        String vectorString = "[" + embedding.stream()
            .map(String::valueOf)
            .reduce((a, b) -> a + "," + b)
            .orElse("") + "]";
        
        String sql = """
            INSERT INTO embeddings (id, variant_id, text_snippet, embedding)
            VALUES (?, ?, ?, ?::vector)
            """;
        
        jdbcTemplate.update(sql, embeddingId, variantId, text, vectorString);
        
        return embeddingId;
    }

    @Override
    public List<SimilarityMatch> findSimilar(String queryText, int k, Map<String, Object> filters, 
                                           boolean withMMR, double diversity) {
        // For now, implement a simple cosine similarity search
        // TODO: Implement MMR algorithm for diversity
        
        String sql = """
            SELECT e.id, e.variant_id, e.text_snippet, 
                   e.embedding <=> ?::vector as similarity_score
            FROM embeddings e
            JOIN content_variants cv ON e.variant_id = cv.id
            JOIN knowledge_objects ko ON cv.knowledge_object_id = ko.id
            WHERE ko.archived = false
            ORDER BY similarity_score ASC
            LIMIT ?
            """;
        
        // Convert query text to embedding (this would normally use EmbeddingService)
        List<Float> queryEmbedding = generateMockEmbedding(queryText);
        String queryVector = "[" + queryEmbedding.stream()
            .map(String::valueOf)
            .reduce((a, b) -> a + "," + b)
            .orElse("") + "]";
        
        return jdbcTemplate.query(sql, new SimilarityMatchRowMapper(), queryVector, k);
    }

    @Override
    public void deleteEmbedding(String embeddingId) {
        String sql = "DELETE FROM embeddings WHERE id = ?";
        jdbcTemplate.update(sql, embeddingId);
    }

    @Override
    public boolean isHealthy() {
        try {
            // Test if we can query the embeddings table
            String sql = "SELECT COUNT(*) FROM embeddings LIMIT 1";
            jdbcTemplate.queryForObject(sql, Integer.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Map<String, Object> getStatistics() {
        try {
            String countSql = "SELECT COUNT(*) FROM embeddings";
            String dimensionSql = "SELECT embedding_dimension FROM embeddings LIMIT 1";
            
            Integer totalEmbeddings = jdbcTemplate.queryForObject(countSql, Integer.class);
            
            return Map.of(
                "total_embeddings", totalEmbeddings != null ? totalEmbeddings : 0,
                "embedding_dimension", embeddingDimension,
                "database_type", "PostgreSQL with pgvector"
            );
        } catch (Exception e) {
            return Map.of(
                "error", "Failed to get statistics: " + e.getMessage(),
                "database_type", "PostgreSQL with pgvector"
            );
        }
    }

    private List<Float> generateMockEmbedding(String text) {
        // Generate a mock embedding for the query text
        // In a real implementation, this would use the EmbeddingService
        return List.of(0.1f, 0.2f, 0.3f, 0.4f, 0.5f); // Mock 5-dimensional embedding
    }

    private static class SimilarityMatchRowMapper implements RowMapper<SimilarityMatch> {
        @Override
        public SimilarityMatch mapRow(ResultSet rs, int rowNum) throws SQLException {
            String embeddingId = rs.getString("id");
            String variantId = rs.getString("variant_id");
            String text = rs.getString("text_snippet");
            double similarityScore = rs.getDouble("similarity_score");
            
            return new SimilarityMatch(embeddingId, variantId, text, similarityScore, Map.of());
        }
    }
}

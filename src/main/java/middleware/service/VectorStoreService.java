package middleware.service;

import java.util.List;
import java.util.Map;

/**
 * Service for vector storage operations including embedding storage and similarity search.
 * Supports both local PostgreSQL with pgvector and production Oracle Vector Search.
 */
public interface VectorStoreService {
    
    /**
     * Store an embedding for a knowledge object variant.
     *
     * @param objectId The knowledge object ID
     * @param variantId The content variant ID
     * @param text The text snippet that was embedded
     * @param embedding The embedding vector
     * @return The embedding ID
     */
    String storeEmbedding(String objectId, String variantId, String text, List<Float> embedding);
    
    /**
     * Find similar knowledge objects using vector similarity.
     *
     * @param queryText The query text to find similar content for
     * @param k Maximum number of results to return
     * @param filters Optional filters (tenant ID, object types, etc.)
     * @param withMMR Whether to use Maximal Marginal Relevance for diversity
     * @param diversity MMR diversity parameter (0.0 to 1.0)
     * @return List of similarity matches
     */
    List<SimilarityMatch> findSimilar(String queryText, int k, Map<String, Object> filters, 
                                     boolean withMMR, double diversity);
    
    /**
     * Find similar knowledge objects with default MMR settings.
     *
     * @param queryText The query text to find similar content for
     * @param k Maximum number of results to return
     * @param filters Optional filters
     * @return List of similarity matches
     */
    default List<SimilarityMatch> findSimilar(String queryText, int k, Map<String, Object> filters) {
        return findSimilar(queryText, k, filters, true, 0.3);
    }
    
    /**
     * Delete an embedding by ID.
     *
     * @param embeddingId The embedding ID to delete
     * @return True if deletion was successful
     */
    boolean deleteEmbedding(String embeddingId);
    
    /**
     * Check if the vector store is healthy.
     *
     * @return True if the service is healthy
     */
    boolean isHealthy();
    
    /**
     * Get the embedding dimension for this vector store.
     *
     * @return The embedding dimension
     */
    int getEmbeddingDimension();
    
    /**
     * Represents a similarity match result.
     */
    class SimilarityMatch {
        private final String objectId;
        private final String variantId;
        private final double similarityScore;
        private final String content;
        private final Map<String, Object> metadata;
        
        public SimilarityMatch(String objectId, String variantId, double similarityScore, 
                             String content, Map<String, Object> metadata) {
            this.objectId = objectId;
            this.variantId = variantId;
            this.similarityScore = similarityScore;
            this.content = content;
            this.metadata = metadata;
        }
        
        public String getObjectId() { return objectId; }
        public String getVariantId() { return variantId; }
        public double getSimilarityScore() { return similarityScore; }
        public String getContent() { return content; }
        public Map<String, Object> getMetadata() { return metadata; }
        
        @Override
        public String toString() {
            return String.format("SimilarityMatch{objectId='%s', score=%.4f}", objectId, similarityScore);
        }
    }
}

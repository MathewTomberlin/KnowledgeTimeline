package middleware.service;

import java.util.List;
import java.util.Map;

/**
 * Service interface for vector storage and similarity search.
 * Provides abstraction for different vector databases (PostgreSQL pgvector, Oracle Vector, etc.).
 */
public interface VectorStoreService {
    
    /**
     * Store an embedding for a knowledge object variant.
     * 
     * @param objectId The knowledge object ID
     * @param variantId The content variant ID
     * @param text The text snippet
     * @param embedding The embedding vector
     * @return The embedding ID
     */
    String storeEmbedding(String objectId, String variantId, String text, List<Float> embedding);
    
    /**
     * Find similar knowledge objects using vector similarity.
     * 
     * @param queryText The query text
     * @param k The number of results to return
     * @param filters Optional filters (tenant, type, etc.)
     * @param withMMR Whether to use Maximal Marginal Relevance for diversity
     * @param diversity The diversity parameter for MMR (0.0 to 1.0)
     * @return List of similarity matches
     */
    List<SimilarityMatch> findSimilar(String queryText, int k, Map<String, Object> filters, 
                                    boolean withMMR, double diversity);
    
    /**
     * Delete an embedding by ID.
     * 
     * @param embeddingId The embedding ID to delete
     */
    void deleteEmbedding(String embeddingId);
    
    /**
     * Check if the vector store is healthy and available.
     * 
     * @return true if the service is healthy
     */
    boolean isHealthy();
    
    /**
     * Get statistics about the vector store.
     * 
     * @return Map of statistics
     */
    Map<String, Object> getStatistics();
    
    /**
     * DTO for similarity search results.
     */
    class SimilarityMatch {
        private String objectId;
        private String variantId;
        private String text;
        private double similarityScore;
        private Map<String, Object> metadata;
        
        public SimilarityMatch(String objectId, String variantId, String text, 
                             double similarityScore, Map<String, Object> metadata) {
            this.objectId = objectId;
            this.variantId = variantId;
            this.text = text;
            this.similarityScore = similarityScore;
            this.metadata = metadata;
        }
        
        // Getters and Setters
        public String getObjectId() {
            return objectId;
        }
        
        public void setObjectId(String objectId) {
            this.objectId = objectId;
        }
        
        public String getVariantId() {
            return variantId;
        }
        
        public void setVariantId(String variantId) {
            this.variantId = variantId;
        }
        
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
        
        public double getSimilarityScore() {
            return similarityScore;
        }
        
        public void setSimilarityScore(double similarityScore) {
            this.similarityScore = similarityScore;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        
        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }
}

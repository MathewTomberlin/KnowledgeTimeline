package middleware.service.impl;

import middleware.service.VectorStoreService;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of VectorStoreService for testing and development.
 * Provides simulated vector storage and similarity search without requiring actual vector database.
 */
@Service
public class MockVectorStoreService implements VectorStoreService {
    
    private final Map<String, StoredEmbedding> embeddings = new ConcurrentHashMap<>();
    private final Map<String, List<String>> objectEmbeddings = new ConcurrentHashMap<>();
    
    @Override
    public String storeEmbedding(String objectId, String variantId, String text, List<Float> embedding) {
        String embeddingId = UUID.randomUUID().toString();
        
        StoredEmbedding storedEmbedding = new StoredEmbedding(
            embeddingId, objectId, variantId, text, embedding, System.currentTimeMillis()
        );
        
        embeddings.put(embeddingId, storedEmbedding);
        
        // Track embeddings by object
        objectEmbeddings.computeIfAbsent(objectId, k -> new ArrayList<>()).add(embeddingId);
        
        return embeddingId;
    }
    
    @Override
    public List<SimilarityMatch> findSimilar(String queryText, int k, Map<String, Object> filters, 
                                           boolean withMMR, double diversity) {
        List<SimilarityMatch> matches = new ArrayList<>();
        
        // Generate a mock query embedding
        List<Float> queryEmbedding = generateMockEmbedding(384);
        
        // Find similar embeddings
        for (StoredEmbedding embedding : embeddings.values()) {
            // Apply filters if provided
            if (filters != null && !matchesFilters(embedding, filters)) {
                continue;
            }
            
            // Calculate mock similarity score
            double similarity = calculateMockSimilarity(queryEmbedding, embedding.embedding);
            
            if (similarity > 0.5) { // Threshold for relevance
                SimilarityMatch match = new SimilarityMatch(
                    embedding.objectId,
                    embedding.variantId,
                    embedding.text,
                    similarity,
                    createMetadata(embedding)
                );
                matches.add(match);
            }
        }
        
        // Sort by similarity and limit to k
        matches.sort((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()));
        
        if (withMMR) {
            matches = applyMMR(matches, diversity);
        }
        
        return matches.subList(0, Math.min(k, matches.size()));
    }
    
    @Override
    public void deleteEmbedding(String embeddingId) {
        StoredEmbedding embedding = embeddings.remove(embeddingId);
        if (embedding != null) {
            objectEmbeddings.get(embedding.objectId).remove(embeddingId);
        }
    }
    
    @Override
    public boolean isHealthy() {
        return true; // Mock service is always healthy
    }
    
    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_embeddings", embeddings.size());
        stats.put("total_objects", objectEmbeddings.size());
        stats.put("average_embeddings_per_object", 
                 objectEmbeddings.values().stream().mapToInt(List::size).average().orElse(0.0));
        return stats;
    }
    
    private List<Float> generateMockEmbedding(int dimensions) {
        List<Float> embedding = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < dimensions; i++) {
            embedding.add(random.nextFloat() * 2 - 1);
        }
        
        return embedding;
    }
    
    private double calculateMockSimilarity(List<Float> embedding1, List<Float> embedding2) {
        // Simple cosine similarity calculation
        if (embedding1.size() != embedding2.size()) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < embedding1.size(); i++) {
            float val1 = embedding1.get(i);
            float val2 = embedding2.get(i);
            
            dotProduct += val1 * val2;
            norm1 += val1 * val1;
            norm2 += val2 * val2;
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    private boolean matchesFilters(StoredEmbedding embedding, Map<String, Object> filters) {
        // Simple filter matching - in a real implementation, this would be more sophisticated
        for (Map.Entry<String, Object> filter : filters.entrySet()) {
            String key = filter.getKey();
            Object value = filter.getValue();
            
            switch (key) {
                case "tenantId":
                    // Mock tenant filtering
                    break;
                case "type":
                    // Mock type filtering
                    break;
                default:
                    // Unknown filter
                    break;
            }
        }
        return true; // Mock implementation always passes filters
    }
    
    private Map<String, Object> createMetadata(StoredEmbedding embedding) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("objectId", embedding.objectId);
        metadata.put("variantId", embedding.variantId);
        metadata.put("createdAt", embedding.createdAt);
        metadata.put("textLength", embedding.text.length());
        return metadata;
    }
    
    private List<SimilarityMatch> applyMMR(List<SimilarityMatch> matches, double diversity) {
        if (matches.isEmpty()) {
            return matches;
        }
        
        List<SimilarityMatch> selected = new ArrayList<>();
        Set<Integer> selectedIndices = new HashSet<>();
        
        // Always select the first (highest similarity) match
        selected.add(matches.get(0));
        selectedIndices.add(0);
        
        // Apply MMR for remaining selections
        for (int i = 1; i < matches.size() && selected.size() < matches.size(); i++) {
            double maxScore = -1;
            int bestIndex = -1;
            
            for (int j = 0; j < matches.size(); j++) {
                if (selectedIndices.contains(j)) {
                    continue;
                }
                
                SimilarityMatch candidate = matches.get(j);
                double relevance = candidate.getSimilarityScore();
                double redundancy = calculateMaxSimilarity(candidate, selected);
                double mmrScore = diversity * relevance - (1 - diversity) * redundancy;
                
                if (mmrScore > maxScore) {
                    maxScore = mmrScore;
                    bestIndex = j;
                }
            }
            
            if (bestIndex != -1) {
                selected.add(matches.get(bestIndex));
                selectedIndices.add(bestIndex);
            }
        }
        
        return selected;
    }
    
    private double calculateMaxSimilarity(SimilarityMatch candidate, List<SimilarityMatch> selected) {
        if (selected.isEmpty()) {
            return 0.0;
        }
        
        double maxSimilarity = 0.0;
        for (SimilarityMatch selectedMatch : selected) {
            // Mock similarity calculation between matches
            double similarity = Math.random() * 0.3; // Mock low similarity between different matches
            maxSimilarity = Math.max(maxSimilarity, similarity);
        }
        
        return maxSimilarity;
    }
    
    private static class StoredEmbedding {
        final String id;
        final String objectId;
        final String variantId;
        final String text;
        final List<Float> embedding;
        final long createdAt;
        
        StoredEmbedding(String id, String objectId, String variantId, String text, 
                       List<Float> embedding, long createdAt) {
            this.id = id;
            this.objectId = objectId;
            this.variantId = variantId;
            this.text = text;
            this.embedding = embedding;
            this.createdAt = createdAt;
        }
    }
}

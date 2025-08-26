package middleware.service.impl;

import middleware.service.EmbeddingService;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Mock implementation of EmbeddingService for testing and development.
 * Provides simulated embeddings without requiring actual embedding API access.
 */
@Service
public class MockEmbeddingService implements EmbeddingService {
    
    private static final int DEFAULT_DIMENSION = 384;
    private static final String MODEL_NAME = "mock-embedding-model";
    
    @Override
    public List<Float> generateEmbedding(String text) {
        return generateMockEmbedding(DEFAULT_DIMENSION);
    }
    
    @Override
    public List<List<Float>> generateEmbeddings(List<String> texts) {
        List<List<Float>> embeddings = new ArrayList<>();
        for (String text : texts) {
            embeddings.add(generateEmbedding(text));
        }
        return embeddings;
    }
    
    @Override
    public int getEmbeddingDimension() {
        return DEFAULT_DIMENSION;
    }
    
    @Override
    public boolean isHealthy() {
        return true; // Mock service is always healthy
    }
    
    @Override
    public String getModelName() {
        return MODEL_NAME;
    }
    
    private List<Float> generateMockEmbedding(int dimensions) {
        List<Float> embedding = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < dimensions; i++) {
            // Generate random values between -1 and 1
            embedding.add(random.nextFloat() * 2 - 1);
        }
        
        return embedding;
    }
}

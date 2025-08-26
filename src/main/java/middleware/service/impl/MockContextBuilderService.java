package middleware.service.impl;

import middleware.service.ContextBuilderService;
import middleware.service.VectorStoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Mock implementation of ContextBuilderService for testing and development.
 * Provides simulated context building without requiring actual knowledge retrieval.
 */
@Service
public class MockContextBuilderService implements ContextBuilderService {
    
    @Autowired
    private MockVectorStoreService vectorStoreService;
    
    private static final int DEFAULT_TOKEN_BUDGET = 2000;
    private static final double DEFAULT_DIVERSITY = 0.3;
    
    @Override
    public String buildContext(String tenantId, String sessionId, String userPrompt, 
                             Map<String, Object> knowledgeContext) {
        // Simulate retrieving relevant knowledge
        List<KnowledgeObject> relevantKnowledge = getRelevantKnowledge(tenantId, userPrompt, 10, null);
        
        // Pack knowledge into context within token budget
        int tokenBudget = getTokenBudget(tenantId);
        return packKnowledgeIntoContext(relevantKnowledge, tokenBudget, DEFAULT_DIVERSITY);
    }
    
    @Override
    public List<KnowledgeObject> getRelevantKnowledge(String tenantId, String query, 
                                                    int maxResults, Map<String, Object> filters) {
        // Use vector store to find similar content
        List<VectorStoreService.SimilarityMatch> matches = vectorStoreService.findSimilar(
            query, maxResults, filters, true, DEFAULT_DIVERSITY
        );
        
        // Convert to KnowledgeObject format
        List<KnowledgeObject> knowledgeObjects = new ArrayList<>();
        for (VectorStoreService.SimilarityMatch match : matches) {
            KnowledgeObject knowledgeObject = new KnowledgeObject(
                match.getObjectId(),
                "SUMMARY", // Mock type
                "Mock Knowledge Object",
                match.getText(),
                "SHORT", // Mock variant type
                match.getSimilarityScore(),
                match.getMetadata()
            );
            knowledgeObjects.add(knowledgeObject);
        }
        
        return knowledgeObjects;
    }
    
    @Override
    public String packKnowledgeIntoContext(List<KnowledgeObject> knowledgeObjects, 
                                        int tokenBudget, double diversity) {
        if (knowledgeObjects.isEmpty()) {
            return "No relevant knowledge found.";
        }
        
        StringBuilder context = new StringBuilder();
        context.append("Relevant knowledge:\n\n");
        
        int currentTokens = 0;
        int maxTokens = tokenBudget - 50; // Reserve some tokens for formatting
        
        for (KnowledgeObject knowledgeObject : knowledgeObjects) {
            String content = knowledgeObject.getContent();
            int estimatedTokens = estimateTokens(content);
            
            if (currentTokens + estimatedTokens > maxTokens) {
                break;
            }
            
            context.append("• ").append(content).append(" [src:").append(knowledgeObject.getId()).append("]\n");
            currentTokens += estimatedTokens;
        }
        
        if (context.length() == 0) {
            return "No relevant knowledge found.";
        }
        
        return context.toString();
    }
    
    @Override
    public int getTokenBudget(String tenantId) {
        // In a real implementation, this would be configurable per tenant
        return DEFAULT_TOKEN_BUDGET;
    }
    
    private int estimateTokens(String text) {
        // Simple token estimation: roughly 4 characters per token
        return Math.max(1, text.length() / 4);
    }
}

package middleware.service.impl;

import middleware.dto.ChatMessage;
import middleware.service.TokenCountingService;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of TokenCountingService for testing and development.
 * Provides simulated token counting without requiring actual tokenization libraries.
 */
@Service
@Primary  // This bean takes precedence when multiple TokenCountingService beans are present
@Profile({"test", "integration"})  // Only active for test and integration profiles
public class MockTokenCountingService implements TokenCountingService {
    
    private static final Map<String, Integer> MODEL_MAX_TOKENS = Map.of(
        "gpt-3.5-turbo", 4096,
        "gpt-4", 8192,
        "claude-3-sonnet", 200000,
        "mock-model", 4096
    );
    
    private static final Map<String, Double> MODEL_COSTS_PER_1K_TOKENS = Map.of(
        "gpt-3.5-turbo", 0.002,
        "gpt-4", 0.03,
        "claude-3-sonnet", 0.015,
        "mock-model", 0.001
    );
    
    @Override
    public int countTokens(String text, String model) {
        // Simple token estimation: roughly 4 characters per token
        return Math.max(1, text.length() / 4);
    }
    
    @Override
    public int countTokensInMessages(List<ChatMessage> messages, String model) {
        int totalTokens = 0;
        for (ChatMessage message : messages) {
            totalTokens += countTokens(message.getContent(), model);
            // Add overhead for role and formatting
            totalTokens += 4;
        }
        return totalTokens;
    }
    
    @Override
    public double estimateCost(int tokens, String model) {
        Double costPer1K = MODEL_COSTS_PER_1K_TOKENS.getOrDefault(model, 0.001);
        return (tokens / 1000.0) * costPer1K;
    }
    
    @Override
    public int getMaxTokens(String model) {
        return MODEL_MAX_TOKENS.getOrDefault(model, 4096);
    }
    
    @Override
    public boolean exceedsTokenLimit(String text, String model) {
        int tokens = countTokens(text, model);
        int maxTokens = getMaxTokens(model);
        return tokens > maxTokens;
    }
    
    @Override
    public String truncateToTokenLimit(String text, String model, int maxTokens) {
        int estimatedTokens = countTokens(text, model);
        if (estimatedTokens <= maxTokens) {
            return text;
        }
        
        // Simple truncation: estimate characters needed
        int maxChars = maxTokens * 4;
        if (text.length() <= maxChars) {
            return text;
        }
        
        // Truncate and add ellipsis
        return text.substring(0, maxChars - 3) + "...";
    }
}

package middleware.service;

/**
 * Service interface for token counting and management.
 * Provides token counting for different models and text content.
 */
public interface TokenCountingService {
    
    /**
     * Count tokens in the given text for the specified model.
     * 
     * @param text The text to count tokens for
     * @param model The model name
     * @return The number of tokens
     */
    int countTokens(String text, String model);
    
    /**
     * Count tokens in a list of messages for the specified model.
     * 
     * @param messages The messages to count tokens for
     * @param model The model name
     * @return The number of tokens
     */
    int countTokensInMessages(java.util.List<middleware.dto.ChatMessage> messages, String model);
    
    /**
     * Estimate the cost for a given number of tokens and model.
     * 
     * @param tokens The number of tokens
     * @param model The model name
     * @return The estimated cost in dollars
     */
    double estimateCost(int tokens, String model);
    
    /**
     * Get the maximum tokens allowed for a model.
     * 
     * @param model The model name
     * @return The maximum tokens
     */
    int getMaxTokens(String model);
    
    /**
     * Check if the text exceeds the maximum tokens for a model.
     * 
     * @param text The text to check
     * @param model The model name
     * @return true if the text exceeds the limit
     */
    boolean exceedsTokenLimit(String text, String model);
    
    /**
     * Truncate text to fit within token limit.
     * 
     * @param text The text to truncate
     * @param model The model name
     * @param maxTokens The maximum tokens allowed
     * @return The truncated text
     */
    String truncateToTokenLimit(String text, String model, int maxTokens);
}

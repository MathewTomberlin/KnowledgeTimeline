package middleware.service;

import middleware.dto.ChatCompletionRequest;
import middleware.dto.ChatCompletionResponse;
import middleware.dto.EmbeddingRequest;
import middleware.dto.EmbeddingResponse;

/**
 * Service interface for LLM client operations.
 * Provides abstraction for different LLM providers (OpenAI, Ollama, etc.).
 */
public interface LLMClientService {
    
    /**
     * Create a chat completion using the configured LLM provider.
     * 
     * @param request The chat completion request
     * @return The chat completion response
     */
    ChatCompletionResponse createChatCompletion(ChatCompletionRequest request);
    
    /**
     * Generate embeddings for the given text.
     * 
     * @param request The embedding request
     * @return The embedding response
     */
    EmbeddingResponse createEmbedding(EmbeddingRequest request);
    
    /**
     * Get available models from the LLM provider.
     * 
     * @return List of available models
     */
    java.util.List<middleware.dto.Model> getAvailableModels();
    
    /**
     * Check if the LLM service is healthy and available.
     * 
     * @return true if the service is healthy
     */
    boolean isHealthy();
}

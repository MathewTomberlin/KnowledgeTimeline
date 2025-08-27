package middleware.service.impl;

import middleware.dto.*;
import middleware.service.LLMClientService;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Mock implementation of LLMClientService for testing and development.
 * Provides simulated responses without requiring actual LLM API access.
 */
@Service
@Primary  // This bean takes precedence when multiple LLMClientService beans are present
@Profile({"test", "integration"})  // Only active for test and integration profiles
public class MockLLMClientService implements LLMClientService {
    
    private static final List<Model> AVAILABLE_MODELS = Arrays.asList(
        Model.builder()
            .id("gpt-3.5-turbo")
            .ownedBy("openai")
            .maxTokens(4096)
            .knowledgeAware(true)
            .build(),
        Model.builder()
            .id("gpt-4")
            .ownedBy("openai")
            .maxTokens(8192)
            .knowledgeAware(true)
            .build(),
        Model.builder()
            .id("claude-3-sonnet")
            .ownedBy("anthropic")
            .maxTokens(200000)
            .knowledgeAware(true)
            .build()
    );
    
    @Override
    public ChatCompletionResponse createChatCompletion(ChatCompletionRequest request) {
        // Generate a mock response based on the request
        String mockResponse = generateMockResponse(request);
        
        ChatMessage assistantMessage = new ChatMessage("assistant", mockResponse);
        ChatChoice choice = new ChatChoice(0, assistantMessage, "stop");
        
        TokenUsage usage = new TokenUsage(
            estimateTokens(request.getMessages()),
            estimateTokens(Collections.singletonList(assistantMessage)),
            estimateTokens(request.getMessages()) + estimateTokens(Collections.singletonList(assistantMessage))
        );
        
        return ChatCompletionResponse.builder()
            .id("mock-" + UUID.randomUUID().toString())
            .model(request.getModel())
            .choices(Collections.singletonList(choice))
            .usage(usage)
            .knowledgeContext(createMockKnowledgeContext())
            .build();
    }
    
    @Override
    public EmbeddingResponse createEmbedding(EmbeddingRequest request) {
        // Generate a mock embedding (384-dimensional vector)
        List<Double> mockEmbedding = generateMockEmbedding(384);
        
        EmbeddingResponse.EmbeddingData embeddingData = 
            new EmbeddingResponse.EmbeddingData(mockEmbedding, 0);
        
        TokenUsage usage = new TokenUsage(estimateTokens(request.getInput()), 0, estimateTokens(request.getInput()));
        
        return EmbeddingResponse.builder()
            .data(Collections.singletonList(embeddingData))
            .model(request.getModel())
            .usage(usage)
            .knowledgeObjectId(request.getStoreKnowledge() ? "mock-knowledge-" + UUID.randomUUID().toString() : null)
            .build();
    }
    
    @Override
    public List<Model> getAvailableModels() {
        return AVAILABLE_MODELS;
    }
    
    @Override
    public boolean isHealthy() {
        return true; // Mock service is always healthy
    }
    
    private String generateMockResponse(ChatCompletionRequest request) {
        String userMessage = request.getMessages().get(request.getMessages().size() - 1).getContent();
        
        // Generate different responses based on the user message
        if (userMessage.toLowerCase().contains("hello") || userMessage.toLowerCase().contains("hi")) {
            return "Hello! I'm a mock LLM service. How can I help you today?";
        } else if (userMessage.toLowerCase().contains("weather")) {
            return "I'm sorry, I'm a mock service and don't have access to real-time weather data. In a real implementation, I would provide current weather information.";
        } else if (userMessage.toLowerCase().contains("knowledge") || userMessage.toLowerCase().contains("context")) {
            return "I'm a knowledge-aware mock service. In a real implementation, I would have access to your knowledge base and provide contextually relevant responses.";
        } else {
            return "This is a mock response from the LLM service. In a real implementation, I would provide a thoughtful and contextual response based on your query: \"" + userMessage + "\"";
        }
    }
    
    private List<Double> generateMockEmbedding(int dimensions) {
        List<Double> embedding = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < dimensions; i++) {
            // Generate random values between -1 and 1
            embedding.add(random.nextDouble() * 2 - 1);
        }
        
        return embedding;
    }
    
    private int estimateTokens(List<ChatMessage> messages) {
        int totalTokens = 0;
        for (ChatMessage message : messages) {
            totalTokens += estimateTokens(message.getContent());
        }
        return totalTokens;
    }
    
    private int estimateTokens(String text) {
        // Simple token estimation: roughly 4 characters per token
        return Math.max(1, text.length() / 4);
    }
    
    private KnowledgeContextResponse createMockKnowledgeContext() {
        List<KnowledgeContextResponse.KnowledgeObjectUsed> objectsUsed = Arrays.asList(
            new KnowledgeContextResponse.KnowledgeObjectUsed("mock-obj-1", "SUMMARY", "Previous conversation summary", 0.85),
            new KnowledgeContextResponse.KnowledgeObjectUsed("mock-obj-2", "FACT", "Relevant fact from knowledge base", 0.72)
        );
        
        return new KnowledgeContextResponse(objectsUsed, 2);
    }
}

package middleware.service.impl;

import middleware.dto.ChatCompletionRequest;
import middleware.dto.ChatCompletionResponse;
import middleware.dto.ChatChoice;
import middleware.dto.ChatMessage;
import middleware.dto.EmbeddingRequest;
import middleware.dto.EmbeddingResponse;
import middleware.dto.Model;
import middleware.dto.TokenUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OpenAIAdapter implementation.
 * Note: These tests focus on the service logic and fallback behavior
 * since WebClient mocking is complex and the service has fallback mechanisms.
 */
@ExtendWith(MockitoExtension.class)
class OpenAIAdapterTest {

    private OpenAIAdapter openAIAdapter;

    @BeforeEach
    void setUp() {
        // Create adapter with test configuration
        openAIAdapter = new OpenAIAdapter("test-api-key", "https://api.openai.com/v1");
    }

    @Test
    void testCreateChatCompletion_WithFallback() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo")
            .messages(Arrays.asList(
                new ChatMessage("user", "Hello, how are you?")
            ))
            .temperature(0.7)
            .maxTokens(100)
            .stream(false)
            .build();

        // When - service will fail to connect and use fallback
        ChatCompletionResponse result = openAIAdapter.createChatCompletion(request);

        // Then
        assertNotNull(result);
        assertEquals("gpt-3.5-turbo", result.getModel());
        assertNotNull(result.getChoices());
        assertEquals(1, result.getChoices().size());
        assertEquals("assistant", result.getChoices().get(0).getMessage().getRole());
        assertTrue(result.getChoices().get(0).getMessage().getContent().contains("mock response"));
        assertNotNull(result.getUsage());
    }

    @Test
    void testCreateChatCompletion_WithEmptyMessages() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo")
            .messages(Arrays.asList())
            .temperature(0.7)
            .maxTokens(100)
            .stream(false)
            .build();

        // When
        ChatCompletionResponse result = openAIAdapter.createChatCompletion(request);

        // Then
        assertNotNull(result);
        assertEquals("gpt-3.5-turbo", result.getModel());
        assertNotNull(result.getChoices());
        assertEquals(1, result.getChoices().size());
    }

    @Test
    void testCreateEmbedding_WithFallback() {
        // Given
        EmbeddingRequest request = EmbeddingRequest.builder()
            .model("text-embedding-ada-002")
            .input("Hello world")
            .build();

        // When - service will fail to connect and use fallback
        EmbeddingResponse result = openAIAdapter.createEmbedding(request);

        // Then
        assertNotNull(result);
        assertEquals("text-embedding-ada-002", result.getModel());
        assertEquals("list", result.getObject());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertNotNull(result.getData().get(0).getEmbedding());
        assertEquals(5, result.getData().get(0).getEmbedding().size()); // Mock 5-dimensional embedding
        assertNotNull(result.getUsage());
    }

    @Test
    void testCreateEmbedding_WithEmptyInput() {
        // Given
        EmbeddingRequest request = EmbeddingRequest.builder()
            .model("text-embedding-ada-002")
            .input("")
            .build();

        // When
        EmbeddingResponse result = openAIAdapter.createEmbedding(request);

        // Then
        assertNotNull(result);
        assertEquals("text-embedding-ada-002", result.getModel());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
    }

    @Test
    void testGetAvailableModels_WithFallback() {
        // When - service will fail to connect and use fallback
        List<Model> result = openAIAdapter.getAvailableModels();

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        
        // Check for expected default models
        List<String> modelIds = result.stream().map(Model::getId).toList();
        assertTrue(modelIds.contains("gpt-3.5-turbo"));
        assertTrue(modelIds.contains("gpt-4"));
        assertTrue(modelIds.contains("text-embedding-ada-002"));
        
        // Check model properties
        result.forEach(model -> {
            assertEquals("model", model.getObject());
            assertEquals("openai", model.getOwnedBy());
        });
    }

    @Test
    void testIsHealthy_DoesNotThrowException() {
        // When - service will try to connect
        // Then - should not throw an exception regardless of connection status
        assertDoesNotThrow(() -> {
            boolean result = openAIAdapter.isHealthy();
            // Result depends on whether the service is actually running
        });
    }

    @Test
    void testCreateChatCompletion_DeterministicMockResponse() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo")
            .messages(Arrays.asList(
                new ChatMessage("user", "Test message")
            ))
            .temperature(0.7)
            .maxTokens(100)
            .stream(false)
            .build();

        // When
        ChatCompletionResponse result1 = openAIAdapter.createChatCompletion(request);
        ChatCompletionResponse result2 = openAIAdapter.createChatCompletion(request);

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(result1.getModel(), result2.getModel());
        assertEquals(result1.getChoices().size(), result2.getChoices().size());
        // The mock response should be consistent in structure but may have different timestamps
    }

    @Test
    void testCreateEmbedding_DeterministicMockResponse() {
        // Given
        EmbeddingRequest request = EmbeddingRequest.builder()
            .model("text-embedding-ada-002")
            .input("Test text")
            .build();

        // When
        EmbeddingResponse result1 = openAIAdapter.createEmbedding(request);
        EmbeddingResponse result2 = openAIAdapter.createEmbedding(request);

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(result1.getModel(), result2.getModel());
        assertEquals(result1.getData().size(), result2.getData().size());
        assertEquals(result1.getData().get(0).getEmbedding().size(), 
                    result2.getData().get(0).getEmbedding().size());
    }

    @Test
    void testCreateChatCompletion_WithMultipleMessages() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("gpt-4")
            .messages(Arrays.asList(
                new ChatMessage("system", "You are a helpful assistant."),
                new ChatMessage("user", "What is the capital of France?"),
                new ChatMessage("assistant", "The capital of France is Paris."),
                new ChatMessage("user", "What about Germany?")
            ))
            .temperature(0.5)
            .maxTokens(150)
            .stream(false)
            .build();

        // When
        ChatCompletionResponse result = openAIAdapter.createChatCompletion(request);

        // Then
        assertNotNull(result);
        assertEquals("gpt-4", result.getModel());
        assertNotNull(result.getChoices());
        assertEquals(1, result.getChoices().size());
        assertNotNull(result.getChoices().get(0).getMessage());
        assertEquals("assistant", result.getChoices().get(0).getMessage().getRole());
    }

    @Test
    void testCreateChatCompletion_WithStreamingEnabled() {
        // Given
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo")
            .messages(Arrays.asList(
                new ChatMessage("user", "Hello")
            ))
            .temperature(0.7)
            .maxTokens(50)
            .stream(true)
            .build();

        // When
        ChatCompletionResponse result = openAIAdapter.createChatCompletion(request);

        // Then
        assertNotNull(result);
        assertEquals("gpt-3.5-turbo", result.getModel());
        assertNotNull(result.getChoices());
        assertEquals(1, result.getChoices().size());
        assertEquals("stop", result.getChoices().get(0).getFinishReason());
    }

    @Test
    void testCreateEmbedding_WithLongText() {
        // Given
        String longText = "This is a very long text that should be embedded. ".repeat(100);
        EmbeddingRequest request = EmbeddingRequest.builder()
            .model("text-embedding-ada-002")
            .input(longText)
            .build();

        // When
        EmbeddingResponse result = openAIAdapter.createEmbedding(request);

        // Then
        assertNotNull(result);
        assertEquals("text-embedding-ada-002", result.getModel());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertNotNull(result.getData().get(0).getEmbedding());
        assertEquals(5, result.getData().get(0).getEmbedding().size()); // Mock 5-dimensional embedding
        assertNotNull(result.getUsage());
        assertEquals(longText.length(), result.getUsage().getPromptTokens());
    }

    @Test
    void testGetAvailableModels_ConsistentDefaultModels() {
        // When
        List<Model> result1 = openAIAdapter.getAvailableModels();
        List<Model> result2 = openAIAdapter.getAvailableModels();

        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(result1.size(), result2.size());
        
        // Should always return the same default models
        List<String> modelIds1 = result1.stream().map(Model::getId).toList();
        List<String> modelIds2 = result2.stream().map(Model::getId).toList();
        assertEquals(modelIds1, modelIds2);
    }
}

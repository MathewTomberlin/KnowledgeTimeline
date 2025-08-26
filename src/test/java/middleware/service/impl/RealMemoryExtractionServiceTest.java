package middleware.service.impl;

import middleware.dto.ChatCompletionRequest;
import middleware.dto.ChatCompletionResponse;
import middleware.dto.ChatMessage;
import middleware.dto.ChatChoice;
import middleware.service.LLMClientService;
import middleware.service.MemoryExtractionService;
import middleware.service.MemoryExtractionService.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RealMemoryExtractionServiceTest {

    @Mock
    private LLMClientService llmClientService;

    @InjectMocks
    private RealMemoryExtractionService memoryExtractionService;

    private ChatCompletionResponse mockResponse;
    private ChatChoice mockChoice;
    private ChatMessage mockMessage;

    @BeforeEach
    void setUp() {
        mockMessage = new ChatMessage("assistant", createValidJsonResponse());
        mockChoice = new ChatChoice();
        mockChoice.setMessage(mockMessage);
        mockChoice.setIndex(0);
        mockChoice.setFinishReason("stop");
        
        mockResponse = new ChatCompletionResponse.Builder()
            .object("chat.completion")
            .model("gpt-3.5-turbo")
            .choices(List.of(mockChoice))
            .usage(new middleware.dto.TokenUsage(100, 50, 150))
            .build();
    }

    @Test
    void testExtractMemory_WithValidInput() {
        // Arrange
        String userMessage = "What is machine learning?";
        String assistantMessage = "Machine learning is a subset of artificial intelligence that enables computers to learn from data.";
        Map<String, Object> context = Map.of("topic", "AI", "session_id", "session1");
        
        when(llmClientService.createChatCompletion(any(ChatCompletionRequest.class)))
            .thenReturn(mockResponse);

        // Act
        MemoryExtraction result = memoryExtractionService.extractMemory(userMessage, assistantMessage, context);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getFacts().size());
        assertEquals(1, result.getEntities().size());
        assertEquals(1, result.getTasks().size());
        assertEquals(0.85, result.getConfidence(), 0.01);
        assertEquals("llm", result.getMetadata().get("extraction_method"));
        
        // Verify LLM was called
        verify(llmClientService).createChatCompletion(any(ChatCompletionRequest.class));
    }

    @Test
    void testExtractMemory_WithNullContext() {
        // Arrange
        String userMessage = "Hello";
        String assistantMessage = "Hi there!";
        
        when(llmClientService.createChatCompletion(any(ChatCompletionRequest.class)))
            .thenReturn(mockResponse);

        // Act
        MemoryExtraction result = memoryExtractionService.extractMemory(userMessage, assistantMessage, null);

        // Assert
        assertNotNull(result);
        assertFalse(result.getFacts().isEmpty());
        assertFalse(result.getEntities().isEmpty());
        assertFalse(result.getTasks().isEmpty());
    }

    @Test
    void testExtractMemory_WhenLLMThrowsException() {
        // Arrange
        String userMessage = "What is AI?";
        String assistantMessage = "AI is artificial intelligence.";
        
        when(llmClientService.createChatCompletion(any(ChatCompletionRequest.class)))
            .thenThrow(new RuntimeException("LLM service unavailable"));

        // Act
        MemoryExtraction result = memoryExtractionService.extractMemory(userMessage, assistantMessage, Map.of());

        // Assert
        assertNotNull(result);
        assertEquals("fallback", result.getMetadata().get("extraction_method"));
        assertEquals("llm_extraction_failed", result.getMetadata().get("error"));
        assertEquals(0.3, result.getConfidence(), 0.01);
        assertFalse(result.getFacts().isEmpty());
    }

    @Test
    void testExtractMemory_WithInvalidJsonResponse() {
        // Arrange
        String userMessage = "What is AI?";
        String assistantMessage = "AI is artificial intelligence.";
        
        ChatMessage invalidMessage = new ChatMessage("assistant", "This is not valid JSON");
        ChatChoice invalidChoice = new ChatChoice();
        invalidChoice.setMessage(invalidMessage);
        invalidChoice.setIndex(0);
        invalidChoice.setFinishReason("stop");
        
        ChatCompletionResponse invalidResponse = new ChatCompletionResponse.Builder()
            .object("chat.completion")
            .model("gpt-3.5-turbo")
            .choices(List.of(invalidChoice))
            .usage(new middleware.dto.TokenUsage(100, 50, 150))
            .build();
        
        when(llmClientService.createChatCompletion(any(ChatCompletionRequest.class)))
            .thenReturn(invalidResponse);

        // Act
        MemoryExtraction result = memoryExtractionService.extractMemory(userMessage, assistantMessage, Map.of());

        // Assert
        assertNotNull(result);
        assertEquals("fallback", result.getMetadata().get("extraction_method"));
    }

    @Test
    void testExtractMemoryBatch_WithValidConversations() {
        // Arrange
        List<ConversationTurn> conversations = Arrays.asList(
            new ConversationTurn("What is ML?", "ML is machine learning.", Map.of("topic", "ML")),
            new ConversationTurn("What is AI?", "AI is artificial intelligence.", Map.of("topic", "AI"))
        );
        
        when(llmClientService.createChatCompletion(any(ChatCompletionRequest.class)))
            .thenReturn(mockResponse);

        // Act
        List<MemoryExtraction> results = memoryExtractionService.extractMemoryBatch(conversations);

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        results.forEach(result -> {
            assertNotNull(result);
            assertFalse(result.getFacts().isEmpty());
            assertFalse(result.getEntities().isEmpty());
            assertFalse(result.getTasks().isEmpty());
        });
        
        // Verify LLM was called twice
        verify(llmClientService, times(2)).createChatCompletion(any(ChatCompletionRequest.class));
    }

    @Test
    void testExtractMemoryBatch_WithEmptyList() {
        // Arrange
        List<ConversationTurn> conversations = new ArrayList<>();

        // Act
        List<MemoryExtraction> results = memoryExtractionService.extractMemoryBatch(conversations);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
        
        // Verify LLM was not called
        verify(llmClientService, never()).createChatCompletion(any(ChatCompletionRequest.class));
    }

    @Test
    void testValidateAndDeduplicateFacts_WithValidFacts() {
        // Arrange
        List<Fact> facts = Arrays.asList(
            new Fact("Machine learning is a subset of AI", "conversation", 0.9),
            new Fact("AI stands for artificial intelligence", "conversation", 0.8),
            new Fact("Machine learning is a subset of AI", "conversation", 0.7) // Duplicate with lower confidence
        );

        // Act
        List<Fact> result = memoryExtractionService.validateAndDeduplicateFacts(facts);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size()); // Should deduplicate
        assertEquals(0.9, result.get(0).getConfidence(), 0.01); // Should keep higher confidence
    }

    @Test
    void testValidateAndDeduplicateFacts_WithNullFacts() {
        // Act
        List<Fact> result = memoryExtractionService.validateAndDeduplicateFacts(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testValidateAndDeduplicateFacts_WithEmptyFacts() {
        // Act
        List<Fact> result = memoryExtractionService.validateAndDeduplicateFacts(new ArrayList<>());

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testValidateAndDeduplicateFacts_WithInvalidFacts() {
        // Arrange
        List<Fact> facts = Arrays.asList(
            new Fact(null, "conversation", 0.8), // Null content
            new Fact("", "conversation", 0.8), // Empty content
            new Fact("Valid fact", "conversation", 1.5), // Invalid confidence
            new Fact("Another valid fact", "conversation", -0.1), // Invalid confidence
            new Fact("Valid fact", "conversation", 0.8) // Valid fact
        );

        // Act
        List<Fact> result = memoryExtractionService.validateAndDeduplicateFacts(facts);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size()); // Only the valid fact should remain
        assertEquals("Valid fact", result.get(0).getContent());
    }

    @Test
    void testValidateAndDeduplicateFacts_WithCaseInsensitiveDeduplication() {
        // Arrange
        List<Fact> facts = Arrays.asList(
            new Fact("Machine Learning is AI", "conversation", 0.9),
            new Fact("machine learning is ai", "conversation", 0.8), // Same content, different case
            new Fact("MACHINE LEARNING IS AI", "conversation", 0.7) // Same content, different case
        );

        // Act
        List<Fact> result = memoryExtractionService.validateAndDeduplicateFacts(facts);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size()); // Should deduplicate case-insensitively
        assertEquals(0.9, result.get(0).getConfidence(), 0.01); // Should keep highest confidence
    }

    @Test
    void testStoreMemoryExtraction() {
        // Arrange
        String tenantId = "tenant1";
        String sessionId = "session1";
        MemoryExtraction extraction = new MemoryExtraction(
            List.of(new Fact("Test fact", "test", 0.8)),
            List.of(new Entity("test", "concept", "A test entity", 0.7)),
            List.of(new Task("Test task", "pending")),
            0.8,
            Map.of("test", "value")
        );

        // Act
        memoryExtractionService.storeMemoryExtraction(tenantId, sessionId, extraction);

        // Assert - Since this uses an in-memory map, we can't easily verify storage
        // But we can verify the method doesn't throw an exception
        assertDoesNotThrow(() -> 
            memoryExtractionService.storeMemoryExtraction(tenantId, sessionId, extraction)
        );
    }

    @Test
    void testExtractMemory_WithComplexContext() {
        // Arrange
        String userMessage = "How does deep learning work?";
        String assistantMessage = "Deep learning uses neural networks with multiple layers to learn complex patterns.";
        Map<String, Object> context = Map.of(
            "topic", "deep learning",
            "session_id", "session1",
            "user_level", "intermediate",
            "previous_topics", List.of("machine learning", "neural networks")
        );
        
        when(llmClientService.createChatCompletion(any(ChatCompletionRequest.class)))
            .thenReturn(mockResponse);

        // Act
        MemoryExtraction result = memoryExtractionService.extractMemory(userMessage, assistantMessage, context);

        // Assert
        assertNotNull(result);
        assertFalse(result.getFacts().isEmpty());
        assertFalse(result.getEntities().isEmpty());
        assertFalse(result.getTasks().isEmpty());
        
        // Verify the context was included in the prompt
        verify(llmClientService).createChatCompletion(argThat(request -> {
            String prompt = request.getMessages().get(0).getContent();
            return prompt.contains("deep learning") && 
                   prompt.contains("intermediate") && 
                   prompt.contains("neural networks");
        }));
    }

    private String createValidJsonResponse() {
        return """
            {
                "facts": [
                    {
                        "content": "Machine learning is a subset of artificial intelligence",
                        "source": "conversation",
                        "confidence": 0.9,
                        "tags": ["AI", "ML"]
                    },
                    {
                        "content": "AI enables computers to learn from data",
                        "source": "conversation",
                        "confidence": 0.8,
                        "tags": ["AI", "data"]
                    }
                ],
                "entities": [
                    {
                        "name": "Machine Learning",
                        "type": "concept",
                        "description": "A subset of AI that enables learning from data",
                        "confidence": 0.85,
                        "attributes": {
                            "category": "technology",
                            "field": "computer science"
                        }
                    }
                ],
                "tasks": [
                    {
                        "description": "Research more about machine learning applications",
                        "status": "pending",
                        "priority": "medium"
                    }
                ],
                "confidence": 0.85
            }
            """;
    }
}

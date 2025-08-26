package middleware.service.impl;

import middleware.dto.ChatMessage;
import middleware.service.TokenCountingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RealTokenCountingService.
 */
@ExtendWith(MockitoExtension.class)
class RealTokenCountingServiceTest {

    @InjectMocks
    private RealTokenCountingService tokenCountingService;

    private ChatMessage userMessage;
    private ChatMessage assistantMessage;
    private ChatMessage systemMessage;

    @BeforeEach
    void setUp() {
        userMessage = new ChatMessage("user", "Hello, how are you today?");
        assistantMessage = new ChatMessage("assistant", "I'm doing well, thank you for asking!");
        systemMessage = new ChatMessage("system", "You are a helpful assistant.");
    }

    @Test
    void testCountTokens_ValidText() {
        // When
        int result = tokenCountingService.countTokens("Hello world", "gpt-3.5-turbo");

        // Then
        assertTrue(result > 0);
        assertTrue(result <= 10); // Should be reasonable for "Hello world"
    }

    @Test
    void testCountTokens_NullText() {
        // When
        int result = tokenCountingService.countTokens(null, "gpt-3.5-turbo");

        // Then
        assertEquals(0, result);
    }

    @Test
    void testCountTokens_EmptyText() {
        // When
        int result = tokenCountingService.countTokens("", "gpt-3.5-turbo");

        // Then
        assertEquals(0, result);
    }

    @Test
    void testCountTokens_WhitespaceText() {
        // When
        int result = tokenCountingService.countTokens("   ", "gpt-3.5-turbo");

        // Then
        assertTrue(result >= 0);
    }

    @Test
    void testCountTokens_DifferentModels() {
        String text = "This is a test message with some content.";
        
        // When
        int gpt35Result = tokenCountingService.countTokens(text, "gpt-3.5-turbo");
        int gpt4Result = tokenCountingService.countTokens(text, "gpt-4");
        int claudeResult = tokenCountingService.countTokens(text, "claude-3-sonnet");
        
        // Then
        assertTrue(gpt35Result > 0);
        assertTrue(gpt4Result > 0);
        assertTrue(claudeResult > 0);
    }

    @Test
    void testCountTokensInMessages_ValidMessages() {
        // Given
        List<ChatMessage> messages = Arrays.asList(systemMessage, userMessage, assistantMessage);

        // When
        int result = tokenCountingService.countTokensInMessages(messages, "gpt-3.5-turbo");

        // Then
        assertTrue(result > 0);
        // Should include tokens for all messages plus formatting overhead
        assertTrue(result >= 20);
    }

    @Test
    void testCountTokensInMessages_NullMessages() {
        // When
        int result = tokenCountingService.countTokensInMessages(null, "gpt-3.5-turbo");

        // Then
        assertEquals(0, result);
    }

    @Test
    void testCountTokensInMessages_EmptyMessages() {
        // When
        int result = tokenCountingService.countTokensInMessages(Arrays.asList(), "gpt-3.5-turbo");

        // Then
        assertEquals(0, result);
    }

    @Test
    void testCountTokensInMessages_MessageWithNullRole() {
        // Given
        ChatMessage messageWithNullRole = new ChatMessage(null, "Some content");
        List<ChatMessage> messages = Arrays.asList(messageWithNullRole);

        // When
        int result = tokenCountingService.countTokensInMessages(messages, "gpt-3.5-turbo");

        // Then
        assertTrue(result > 0);
        // Should count content tokens plus formatting overhead
        assertTrue(result >= 5);
    }

    @Test
    void testCountTokensInMessages_MessageWithNullContent() {
        // Given
        ChatMessage messageWithNullContent = new ChatMessage("user", null);
        List<ChatMessage> messages = Arrays.asList(messageWithNullContent);

        // When
        int result = tokenCountingService.countTokensInMessages(messages, "gpt-3.5-turbo");

        // Then
        assertTrue(result > 0);
        // Should count role tokens plus formatting overhead
        assertTrue(result >= 5);
    }

    @Test
    void testEstimateCost_ValidTokens() {
        // When
        double gpt35Cost = tokenCountingService.estimateCost(1000, "gpt-3.5-turbo");
        double gpt4Cost = tokenCountingService.estimateCost(1000, "gpt-4");
        double claudeCost = tokenCountingService.estimateCost(1000, "claude-3-sonnet");

        // Then
        assertEquals(0.0015, gpt35Cost, 0.0001); // $0.0015 per 1K tokens
        assertEquals(0.03, gpt4Cost, 0.0001); // $0.03 per 1K tokens
        assertEquals(0.003, claudeCost, 0.0001); // $0.003 per 1K tokens
    }

    @Test
    void testEstimateCost_ZeroTokens() {
        // When
        double result = tokenCountingService.estimateCost(0, "gpt-3.5-turbo");

        // Then
        assertEquals(0.0, result, 0.0001);
    }

    @Test
    void testEstimateCost_UnknownModel() {
        // When
        double result = tokenCountingService.estimateCost(1000, "unknown-model");

        // Then
        assertEquals(0.0015, result, 0.0001); // Should default to GPT-3.5 pricing
    }

    @Test
    void testGetMaxTokens_KnownModels() {
        // When
        int gpt35Max = tokenCountingService.getMaxTokens("gpt-3.5-turbo");
        int gpt4Max = tokenCountingService.getMaxTokens("gpt-4");
        int gpt4TurboMax = tokenCountingService.getMaxTokens("gpt-4-turbo");
        int claudeMax = tokenCountingService.getMaxTokens("claude-3-opus");

        // Then
        assertEquals(4096, gpt35Max);
        assertEquals(8192, gpt4Max);
        assertEquals(128000, gpt4TurboMax);
        assertEquals(200000, claudeMax);
    }

    @Test
    void testGetMaxTokens_UnknownModel() {
        // When
        int result = tokenCountingService.getMaxTokens("unknown-model");

        // Then
        assertEquals(4096, result); // Should default to 4K tokens
    }

    @Test
    void testExceedsTokenLimit_WithinLimit() {
        // Given
        String shortText = "This is a short message.";

        // When
        boolean result = tokenCountingService.exceedsTokenLimit(shortText, "gpt-3.5-turbo");

        // Then
        assertFalse(result);
    }

    @Test
    void testExceedsTokenLimit_ExceedsLimit() {
        // Given
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longText.append("This is a very long message that should exceed the token limit. ");
        }

        // When
        boolean result = tokenCountingService.exceedsTokenLimit(longText.toString(), "gpt-3.5-turbo");

        // Then
        assertTrue(result);
    }

    @Test
    void testExceedsTokenLimit_NullText() {
        // When
        boolean result = tokenCountingService.exceedsTokenLimit(null, "gpt-3.5-turbo");

        // Then
        assertFalse(result);
    }

    @Test
    void testExceedsTokenLimit_EmptyText() {
        // When
        boolean result = tokenCountingService.exceedsTokenLimit("", "gpt-3.5-turbo");

        // Then
        assertFalse(result);
    }

    @Test
    void testTruncateToTokenLimit_WithinLimit() {
        // Given
        String shortText = "This is a short message.";

        // When
        String result = tokenCountingService.truncateToTokenLimit(shortText, "gpt-3.5-turbo", 100);

        // Then
        assertEquals(shortText, result);
    }

    @Test
    void testTruncateToTokenLimit_ExceedsLimit() {
        // Given
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longText.append("This is a very long message that should exceed the token limit. ");
        }

        // When
        String result = tokenCountingService.truncateToTokenLimit(longText.toString(), "gpt-3.5-turbo", 100);

        // Then
        assertNotNull(result);
        assertTrue(result.length() < longText.length());
        assertTrue(tokenCountingService.countTokens(result, "gpt-3.5-turbo") <= 100);
    }

    @Test
    void testTruncateToTokenLimit_NullText() {
        // When
        String result = tokenCountingService.truncateToTokenLimit(null, "gpt-3.5-turbo", 100);

        // Then
        assertNull(result);
    }

    @Test
    void testTruncateToTokenLimit_EmptyText() {
        // When
        String result = tokenCountingService.truncateToTokenLimit("", "gpt-3.5-turbo", 100);

        // Then
        assertEquals("", result);
    }

    @Test
    void testTruncateToTokenLimit_ZeroMaxTokens() {
        // Given
        String text = "This is a test message.";

        // When
        String result = tokenCountingService.truncateToTokenLimit(text, "gpt-3.5-turbo", 0);

        // Then
        assertEquals("", result);
    }

    @Test
    void testTokenCountingConsistency() {
        // Given
        String text = "The quick brown fox jumps over the lazy dog.";

        // When
        int tokens1 = tokenCountingService.countTokens(text, "gpt-3.5-turbo");
        int tokens2 = tokenCountingService.countTokens(text, "gpt-3.5-turbo");

        // Then
        assertEquals(tokens1, tokens2); // Should be consistent
    }

    @Test
    void testModelSpecificTokenization() {
        // Given
        String text = "Hello world! This is a test message.";

        // When
        int gpt35Tokens = tokenCountingService.countTokens(text, "gpt-3.5-turbo");
        int claudeTokens = tokenCountingService.countTokens(text, "claude-3-sonnet");
        int llamaTokens = tokenCountingService.countTokens(text, "llama-2-7b");

        // Then
        assertTrue(gpt35Tokens > 0);
        assertTrue(claudeTokens > 0);
        assertTrue(llamaTokens > 0);
        
        // Different models may have different tokenization, but all should be reasonable
        assertTrue(gpt35Tokens >= 8 && gpt35Tokens <= 20);
        assertTrue(claudeTokens >= 8 && claudeTokens <= 20);
        assertTrue(llamaTokens >= 8 && llamaTokens <= 20);
    }
}

package middleware.service.impl;

import middleware.model.DialogueState;
import middleware.model.KnowledgeObject;
import middleware.model.KnowledgeObjectType;
import middleware.repository.DialogueStateRepository;
import middleware.repository.KnowledgeObjectRepository;
import middleware.service.LLMClientService;
import middleware.service.TokenCountingService;
import middleware.dto.ChatCompletionRequest;
import middleware.dto.ChatCompletionResponse;
import middleware.dto.ChatChoice;
import middleware.dto.ChatMessage;
import middleware.dto.TokenUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SessionSummarizationService.
 */
@ExtendWith(MockitoExtension.class)
class SessionSummarizationServiceTest {

    @Mock
    private DialogueStateRepository dialogueStateRepository;

    @Mock
    private KnowledgeObjectRepository knowledgeObjectRepository;

    @Mock
    private LLMClientService llmClientService;

    @Mock
    private TokenCountingService tokenCountingService;

    private SessionSummarizationService sessionSummarizationService;

    @BeforeEach
    void setUp() {
        sessionSummarizationService = new SessionSummarizationService(
            dialogueStateRepository,
            knowledgeObjectRepository,
            llmClientService,
            tokenCountingService
        );
    }

    @Test
    void testSummarizeSession_Success() {
        // Arrange
        String sessionId = "test-session-123";
        String tenantId = "test-tenant";
        String userId = "test-user";
        
        DialogueState dialogueState = new DialogueState();
        dialogueState.setId(UUID.randomUUID().toString());
        dialogueState.setTenantId(tenantId);
        dialogueState.setSessionId(sessionId);
        dialogueState.setUserId(userId);
        dialogueState.setSummaryShort("Previous summary");
        dialogueState.setSummaryBullets("Previous bullets");
        dialogueState.setTopics("[\"topic1\", \"topic2\"]");
        dialogueState.setTurnCount(5);
        dialogueState.setCumulativeTokens(1000);
        dialogueState.setLastUpdatedAt(LocalDateTime.now());

        String expectedSummary = "This is a comprehensive summary of the conversation.";
        
        ChatMessage responseMessage = new ChatMessage("assistant", expectedSummary);
        ChatChoice choice = new ChatChoice();
        choice.setMessage(responseMessage);
        choice.setFinishReason("stop");
        choice.setIndex(0);
        
        ChatCompletionResponse llmResponse = new ChatCompletionResponse();
        llmResponse.setId("chatcmpl-123");
        llmResponse.setObject("chat.completion");
        llmResponse.setCreated(System.currentTimeMillis());
        llmResponse.setModel("gpt-3.5-turbo");
        llmResponse.setChoices(List.of(choice));
        llmResponse.setUsage(new TokenUsage(50, 100, 150));

        KnowledgeObject savedMemory = new KnowledgeObject();
        savedMemory.setId(UUID.randomUUID().toString());
        savedMemory.setTenantId(tenantId);
        savedMemory.setType(KnowledgeObjectType.SESSION_MEMORY);
        savedMemory.setSessionId(sessionId);

        // Mock repository calls
        when(dialogueStateRepository.findBySessionIdAndTenantId(sessionId, tenantId))
            .thenReturn(Optional.of(dialogueState));
        when(knowledgeObjectRepository.save(any(KnowledgeObject.class)))
            .thenReturn(savedMemory);
        when(dialogueStateRepository.save(any(DialogueState.class)))
            .thenReturn(dialogueState);
        when(llmClientService.createChatCompletion(any(ChatCompletionRequest.class)))
            .thenReturn(llmResponse);
        when(tokenCountingService.countTokens(anyString(), anyString()))
            .thenReturn(150);

        // Act
        Map<String, Object> result = sessionSummarizationService.summarizeSession(sessionId, tenantId);

        // Assert
        assertNotNull(result);
        assertEquals("completed", result.get("status"));
        assertEquals(sessionId, result.get("session_id"));
        assertEquals(expectedSummary, result.get("summary"));
        assertEquals(savedMemory.getId(), result.get("memory_object_id"));
        assertEquals(150, result.get("tokens_used"));

        // Verify repository calls
        verify(dialogueStateRepository).findBySessionIdAndTenantId(sessionId, tenantId);
        verify(knowledgeObjectRepository).save(any(KnowledgeObject.class));
        verify(dialogueStateRepository).save(any(DialogueState.class));
        verify(llmClientService).createChatCompletion(any(ChatCompletionRequest.class));
        verify(tokenCountingService, times(2)).countTokens(anyString(), eq("gpt-3.5-turbo"));
    }

    @Test
    void testSummarizeSession_SessionNotFound() {
        // Arrange
        String sessionId = "non-existent-session";
        String tenantId = "test-tenant";

        when(dialogueStateRepository.findBySessionIdAndTenantId(sessionId, tenantId))
            .thenReturn(Optional.empty());

        // Act
        Map<String, Object> result = sessionSummarizationService.summarizeSession(sessionId, tenantId);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("error"));
        assertEquals("Session not found", result.get("error"));
        assertEquals(sessionId, result.get("session_id"));

        // Verify no other calls were made
        verify(knowledgeObjectRepository, never()).save(any());
        verify(llmClientService, never()).createChatCompletion(any());
    }

    @Test
    void testSummarizeSession_LLMFailure() {
        // Arrange
        String sessionId = "test-session-123";
        String tenantId = "test-tenant";
        
        DialogueState dialogueState = new DialogueState();
        dialogueState.setId(UUID.randomUUID().toString());
        dialogueState.setTenantId(tenantId);
        dialogueState.setSessionId(sessionId);
        dialogueState.setSummaryShort("Fallback summary");
        dialogueState.setTurnCount(3);
        dialogueState.setCumulativeTokens(500);
        dialogueState.setLastUpdatedAt(LocalDateTime.now());

        KnowledgeObject savedMemory = new KnowledgeObject();
        savedMemory.setId(UUID.randomUUID().toString());

        // Mock repository calls
        when(dialogueStateRepository.findBySessionIdAndTenantId(sessionId, tenantId))
            .thenReturn(Optional.of(dialogueState));
        when(knowledgeObjectRepository.save(any(KnowledgeObject.class)))
            .thenReturn(savedMemory);
        when(dialogueStateRepository.save(any(DialogueState.class)))
            .thenReturn(dialogueState);
        when(llmClientService.createChatCompletion(any(ChatCompletionRequest.class)))
            .thenReturn(null); // LLM service fails
        when(tokenCountingService.countTokens(anyString(), anyString()))
            .thenReturn(50);

        // Act
        Map<String, Object> result = sessionSummarizationService.summarizeSession(sessionId, tenantId);

        // Assert
        assertNotNull(result);
        assertEquals("completed", result.get("status"));
        assertEquals("Fallback summary", result.get("summary"));
        assertEquals(savedMemory.getId(), result.get("memory_object_id"));

        // Verify fallback behavior
        verify(llmClientService).createChatCompletion(any(ChatCompletionRequest.class));
    }

    @Test
    void testBatchSummarizeSessions_Success() {
        // Arrange
        String tenantId = "test-tenant";
        List<String> sessionIds = List.of("session1", "session2", "session3");

        DialogueState dialogueState = new DialogueState();
        dialogueState.setId(UUID.randomUUID().toString());
        dialogueState.setTenantId(tenantId);
        dialogueState.setSessionId("session1");
        dialogueState.setSummaryShort("Summary");
        dialogueState.setTurnCount(2);
        dialogueState.setCumulativeTokens(300);
        dialogueState.setLastUpdatedAt(LocalDateTime.now());

        ChatMessage responseMessage = new ChatMessage("assistant", "Generated summary");
        ChatChoice choice = new ChatChoice();
        choice.setMessage(responseMessage);
        choice.setFinishReason("stop");
        choice.setIndex(0);
        
        ChatCompletionResponse llmResponse = new ChatCompletionResponse();
        llmResponse.setChoices(List.of(choice));

        KnowledgeObject savedMemory = new KnowledgeObject();
        savedMemory.setId(UUID.randomUUID().toString());

        // Mock repository calls
        when(dialogueStateRepository.findBySessionIdAndTenantId(anyString(), eq(tenantId)))
            .thenReturn(Optional.of(dialogueState));
        when(knowledgeObjectRepository.save(any(KnowledgeObject.class)))
            .thenReturn(savedMemory);
        when(dialogueStateRepository.save(any(DialogueState.class)))
            .thenReturn(dialogueState);
        when(llmClientService.createChatCompletion(any(ChatCompletionRequest.class)))
            .thenReturn(llmResponse);
        when(tokenCountingService.countTokens(anyString(), anyString()))
            .thenReturn(100);

        // Act
        Map<String, Object> result = sessionSummarizationService.batchSummarizeSessions(tenantId, sessionIds);

        // Assert
        assertNotNull(result);
        assertEquals("completed", result.get("status"));
        assertEquals(3, result.get("total_sessions"));
        assertEquals(3, result.get("successful"));
        assertEquals(0, result.get("failed"));

        // Verify calls for each session
        verify(dialogueStateRepository, times(3)).findBySessionIdAndTenantId(anyString(), eq(tenantId));
        verify(knowledgeObjectRepository, times(3)).save(any(KnowledgeObject.class));
    }

    @Test
    void testBatchSummarizeSessions_WithFailures() {
        // Arrange
        String tenantId = "test-tenant";
        List<String> sessionIds = List.of("session1", "session2", "session3");

        DialogueState dialogueState = new DialogueState();
        dialogueState.setId(UUID.randomUUID().toString());
        dialogueState.setTenantId(tenantId);
        dialogueState.setSessionId("session1");
        dialogueState.setSummaryShort("Summary");
        dialogueState.setTurnCount(2);
        dialogueState.setCumulativeTokens(300);
        dialogueState.setLastUpdatedAt(LocalDateTime.now());

        ChatMessage responseMessage = new ChatMessage("assistant", "Generated summary");
        ChatChoice choice = new ChatChoice();
        choice.setMessage(responseMessage);
        choice.setFinishReason("stop");
        choice.setIndex(0);
        
        ChatCompletionResponse llmResponse = new ChatCompletionResponse();
        llmResponse.setChoices(List.of(choice));

        KnowledgeObject savedMemory = new KnowledgeObject();
        savedMemory.setId(UUID.randomUUID().toString());

        // Mock repository calls - session2 will fail
        when(dialogueStateRepository.findBySessionIdAndTenantId("session1", tenantId))
            .thenReturn(Optional.of(dialogueState));
        when(dialogueStateRepository.findBySessionIdAndTenantId("session2", tenantId))
            .thenReturn(Optional.empty()); // Session not found
        when(dialogueStateRepository.findBySessionIdAndTenantId("session3", tenantId))
            .thenReturn(Optional.of(dialogueState));
        
        when(knowledgeObjectRepository.save(any(KnowledgeObject.class)))
            .thenReturn(savedMemory);
        when(dialogueStateRepository.save(any(DialogueState.class)))
            .thenReturn(dialogueState);
        when(llmClientService.createChatCompletion(any(ChatCompletionRequest.class)))
            .thenReturn(llmResponse);
        when(tokenCountingService.countTokens(anyString(), anyString()))
            .thenReturn(100);

        // Act
        Map<String, Object> result = sessionSummarizationService.batchSummarizeSessions(tenantId, sessionIds);

        // Assert
        assertNotNull(result);
        assertEquals("completed", result.get("status"));
        assertEquals(3, result.get("total_sessions"));
        assertEquals(2, result.get("successful"));
        assertEquals(1, result.get("failed"));
    }
}

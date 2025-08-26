package middleware.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import middleware.model.DialogueState;
import middleware.repository.DialogueStateRepository;
import middleware.service.DialogueStateService;
import middleware.service.LLMClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for RealDialogueStateService implementation.
 */
@ExtendWith(MockitoExtension.class)
class RealDialogueStateServiceTest {

    @Mock
    private DialogueStateRepository dialogueStateRepository;

    @Mock
    private LLMClientService llmClientService;

    private ObjectMapper objectMapper;
    private RealDialogueStateService dialogueStateService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        dialogueStateService = new RealDialogueStateService(dialogueStateRepository, llmClientService, objectMapper);
    }

    @Test
    void testGetOrCreateDialogueState_ExistingState() {
        // Given
        String tenantId = "tenant1";
        String sessionId = "session1";
        String userId = "user1";
        
        DialogueState existingState = new DialogueState(tenantId, sessionId, userId);
        existingState.setId("existing-id");
        existingState.setSummaryShort("Existing summary");
        existingState.setSummaryBullets("Existing bullets");
        existingState.setTopics("[\"topic1\", \"topic2\"]");
        existingState.setCumulativeTokens(100);
        existingState.setLastUpdatedAt(LocalDateTime.now());
        
        when(dialogueStateRepository.findBySessionIdAndTenantId(sessionId, tenantId))
            .thenReturn(Optional.of(existingState));

        // When
        DialogueStateService.DialogueState result = dialogueStateService.getOrCreateDialogueState(tenantId, sessionId, userId);

        // Then
        assertNotNull(result);
        assertEquals("existing-id", result.getId());
        assertEquals(tenantId, result.getTenantId());
        assertEquals(sessionId, result.getSessionId());
        assertEquals(userId, result.getUserId());
        assertEquals("Existing summary", result.getSummaryShort());
        assertEquals("Existing bullets", result.getSummaryBullets());
        assertEquals(100, result.getCumulativeTokens());
        assertEquals(2, result.getTopics().size());
        assertTrue(result.getTopics().contains("topic1"));
        assertTrue(result.getTopics().contains("topic2"));
        
        verify(dialogueStateRepository).findBySessionIdAndTenantId(sessionId, tenantId);
        verify(dialogueStateRepository, never()).save(any());
    }

    @Test
    void testGetOrCreateDialogueState_NewState() {
        // Given
        String tenantId = "tenant1";
        String sessionId = "session1";
        String userId = "user1";
        
        when(dialogueStateRepository.findBySessionIdAndTenantId(sessionId, tenantId))
            .thenReturn(Optional.empty());
        
        DialogueState newState = new DialogueState(tenantId, sessionId, userId);
        newState.setId("new-id");
        when(dialogueStateRepository.save(any(DialogueState.class)))
            .thenReturn(newState);

        // When
        DialogueStateService.DialogueState result = dialogueStateService.getOrCreateDialogueState(tenantId, sessionId, userId);

        // Then
        assertNotNull(result);
        assertEquals("new-id", result.getId());
        assertEquals(tenantId, result.getTenantId());
        assertEquals(sessionId, result.getSessionId());
        assertEquals(userId, result.getUserId());
        assertEquals(0, result.getCumulativeTokens());
        assertTrue(result.getTopics().isEmpty());
        
        verify(dialogueStateRepository).findBySessionIdAndTenantId(sessionId, tenantId);
        verify(dialogueStateRepository).save(any(DialogueState.class));
    }

    @Test
    void testUpdateDialogueState_InterfaceLimitation() {
        // Given
        String sessionId = "session1";
        String userMessage = "Hello";
        String assistantMessage = "Hi there!";
        int knowledgeTokens = 50;

        // When
        dialogueStateService.updateDialogueState(sessionId, userMessage, assistantMessage, knowledgeTokens);

        // Then
        // The current implementation logs a warning and returns early due to interface limitations
        // This test verifies the method doesn't throw exceptions
        verify(dialogueStateRepository, never()).findById(any());
        verify(dialogueStateRepository, never()).save(any());
    }

    @Test
    void testSummarizeDialogueState_InterfaceLimitation() {
        // Given
        String sessionId = "session1";

        // When
        DialogueStateService.DialogueState result = dialogueStateService.summarizeDialogueState(sessionId);

        // Then
        // The current implementation logs a warning and returns null due to interface limitations
        assertNull(result);
        verify(dialogueStateRepository, never()).findById(any());
        verify(llmClientService, never()).createChatCompletion(any());
    }

    @Test
    void testGetRecentDialogueStates() {
        // Given
        String tenantId = "tenant1";
        String userId = "user1";
        int limit = 5;
        
        DialogueState state1 = new DialogueState(tenantId, "session1", userId);
        state1.setId("id1");
        state1.setLastUpdatedAt(LocalDateTime.now().minusHours(1));
        
        DialogueState state2 = new DialogueState(tenantId, "session2", userId);
        state2.setId("id2");
        state2.setLastUpdatedAt(LocalDateTime.now());
        
        List<DialogueState> states = Arrays.asList(state2, state1); // Ordered by lastUpdatedAt DESC
        
        when(dialogueStateRepository.findRecentByUserIdAndTenantId(eq(userId), eq(tenantId), any(PageRequest.class)))
            .thenReturn(states);

        // When
        List<DialogueStateService.DialogueState> result = dialogueStateService.getRecentDialogueStates(tenantId, userId, limit);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("id2", result.get(0).getId());
        assertEquals("id1", result.get(1).getId());
        
        verify(dialogueStateRepository).findRecentByUserIdAndTenantId(eq(userId), eq(tenantId), any(PageRequest.class));
    }

    @Test
    void testCleanupOldDialogueStates() {
        // Given
        String tenantId = "tenant1";
        int olderThanDays = 30;
        int deletedCount = 5;
        
        when(dialogueStateRepository.deleteOldByTenantId(eq(tenantId), any(LocalDateTime.class)))
            .thenReturn(deletedCount);

        // When
        int result = dialogueStateService.cleanupOldDialogueStates(tenantId, olderThanDays);

        // Then
        assertEquals(deletedCount, result);
        verify(dialogueStateRepository).deleteOldByTenantId(eq(tenantId), any(LocalDateTime.class));
    }

    @Test
    void testConvertToDto_WithNullFields() {
        // Given
        DialogueState entity = new DialogueState("tenant1", "session1", "user1");
        entity.setId("test-id");
        entity.setSummaryShort(null);
        entity.setSummaryBullets(null);
        entity.setTopics(null);
        entity.setMetadata(null);
        entity.setCumulativeTokens(null);
        entity.setLastUpdatedAt(LocalDateTime.now());

        when(dialogueStateRepository.findBySessionIdAndTenantId("session1", "tenant1"))
            .thenReturn(Optional.of(entity));

        // When
        DialogueStateService.DialogueState dto = dialogueStateService.getOrCreateDialogueState("tenant1", "session1", "user1");

        // Then
        // This test verifies that the service handles null fields gracefully
        assertNotNull(dto);
        assertEquals("test-id", dto.getId());
        assertEquals("tenant1", dto.getTenantId());
        assertEquals("session1", dto.getSessionId());
        assertEquals("user1", dto.getUserId());
        assertTrue(dto.getTopics().isEmpty());
    }

    @Test
    void testGetRecentDialogueStates_EmptyResult() {
        // Given
        String tenantId = "tenant1";
        String userId = "user1";
        int limit = 5;
        
        when(dialogueStateRepository.findRecentByUserIdAndTenantId(eq(userId), eq(tenantId), any(PageRequest.class)))
            .thenReturn(Arrays.asList());

        // When
        List<DialogueStateService.DialogueState> result = dialogueStateService.getRecentDialogueStates(tenantId, userId, limit);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(dialogueStateRepository).findRecentByUserIdAndTenantId(eq(userId), eq(tenantId), any(PageRequest.class));
    }

    @Test
    void testCleanupOldDialogueStates_NoDeletions() {
        // Given
        String tenantId = "tenant1";
        int olderThanDays = 30;
        
        when(dialogueStateRepository.deleteOldByTenantId(eq(tenantId), any(LocalDateTime.class)))
            .thenReturn(0);

        // When
        int result = dialogueStateService.cleanupOldDialogueStates(tenantId, olderThanDays);

        // Then
        assertEquals(0, result);
        verify(dialogueStateRepository).deleteOldByTenantId(eq(tenantId), any(LocalDateTime.class));
    }
}

package middleware.service.impl;

import middleware.service.DialogueStateService;
import middleware.service.DialogueStateService.DialogueState;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of DialogueStateService for testing and local development.
 * Provides simulated dialogue state management functionality.
 */
@Service
public class MockDialogueStateService implements DialogueStateService {

    private final ConcurrentHashMap<String, DialogueState> dialogueStates = new ConcurrentHashMap<>();

    @Override
    public DialogueState getOrCreateDialogueState(String tenantId, String sessionId, String userId) {
        String key = tenantId + ":" + sessionId;
        return dialogueStates.computeIfAbsent(key, k -> new DialogueState(
            java.util.UUID.randomUUID().toString(),
            tenantId,
            sessionId,
            userId
        ));
    }

    @Override
    public void updateDialogueState(String sessionId, String userMessage, String assistantMessage, int knowledgeTokens) {
        // Find the dialogue state by session ID
        DialogueState state = dialogueStates.values().stream()
            .filter(s -> s.getSessionId().equals(sessionId))
            .findFirst()
            .orElse(null);
        
        if (state != null) {
            state.setCumulativeTokens(state.getCumulativeTokens() + knowledgeTokens);
            state.setLastUpdatedAt(LocalDateTime.now());
        }
    }

    @Override
    public DialogueState summarizeDialogueState(String sessionId) {
        // Find the dialogue state by session ID
        DialogueState state = dialogueStates.values().stream()
            .filter(s -> s.getSessionId().equals(sessionId))
            .findFirst()
            .orElse(null);
        
        if (state != null) {
            // Mock summarization
            state.setSummaryShort("Mock summarized dialogue");
            state.setSummaryBullets("Mock bullet point 1\nMock bullet point 2");
            state.setLastUpdatedAt(LocalDateTime.now());
        }
        
        return state;
    }

    @Override
    public List<DialogueState> getRecentDialogueStates(String tenantId, String userId, int limit) {
        return dialogueStates.values().stream()
            .filter(state -> state.getTenantId().equals(tenantId) && state.getUserId().equals(userId))
            .sorted((a, b) -> b.getLastUpdatedAt().compareTo(a.getLastUpdatedAt()))
            .limit(limit)
            .toList();
    }

    @Override
    public int cleanupOldDialogueStates(String tenantId, int olderThanDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(olderThanDays);
        final int[] removed = {0};
        
        dialogueStates.entrySet().removeIf(entry -> {
            DialogueState state = entry.getValue();
            if (state.getTenantId().equals(tenantId) && state.getLastUpdatedAt().isBefore(cutoffDate)) {
                removed[0]++;
                return true;
            }
            return false;
        });
        
        return removed[0];
    }
}

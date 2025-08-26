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
import middleware.dto.ChatMessage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for summarizing dialogue sessions and creating session memory objects.
 * Uses LLM to generate summaries and extracts key information from conversations.
 */
@Service
public class SessionSummarizationService {

    private final DialogueStateRepository dialogueStateRepository;
    private final KnowledgeObjectRepository knowledgeObjectRepository;
    private final LLMClientService llmClientService;
    private final TokenCountingService tokenCountingService;

    public SessionSummarizationService(DialogueStateRepository dialogueStateRepository,
                                     KnowledgeObjectRepository knowledgeObjectRepository,
                                     LLMClientService llmClientService,
                                     TokenCountingService tokenCountingService) {
        this.dialogueStateRepository = dialogueStateRepository;
        this.knowledgeObjectRepository = knowledgeObjectRepository;
        this.llmClientService = llmClientService;
        this.tokenCountingService = tokenCountingService;
    }

    /**
     * Summarize a dialogue session and create session memory objects.
     *
     * @param sessionId The session identifier
     * @param tenantId The tenant identifier
     * @return Summary information
     */
    public Map<String, Object> summarizeSession(String sessionId, String tenantId) {
        try {
            // Get the dialogue state for the session
            var dialogueStateOpt = dialogueStateRepository.findBySessionIdAndTenantId(sessionId, tenantId);
            if (dialogueStateOpt.isEmpty()) {
                return Map.of("error", "Session not found", "session_id", sessionId);
            }

            DialogueState dialogueState = dialogueStateOpt.get();
            
            // Generate summary using LLM
            String summary = generateSummary(dialogueState);
            
            // Create session memory knowledge object
            KnowledgeObject sessionMemory = createSessionMemory(sessionId, tenantId, dialogueState, summary);
            
            // Update dialogue state with summary
            updateDialogueState(dialogueState, summary);
            
            return Map.of(
                "status", "completed",
                "session_id", sessionId,
                "summary", summary,
                "memory_object_id", sessionMemory.getId(),
                "tokens_used", tokenCountingService.countTokens(summary, "gpt-3.5-turbo")
            );
            
        } catch (Exception e) {
            return Map.of("error", "Failed to summarize session: " + e.getMessage(), "session_id", sessionId);
        }
    }

    /**
     * Generate a summary of the dialogue using LLM.
     *
     * @param dialogueState The dialogue state to summarize
     * @return Generated summary
     */
    private String generateSummary(DialogueState dialogueState) {
        // Build prompt for summarization
        String prompt = buildSummarizationPrompt(dialogueState);
        
        // Create chat completion request
        ChatCompletionRequest request = ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo")
            .messages(List.of(new ChatMessage("user", prompt)))
            .temperature(0.3)
            .maxTokens(500)
            .stream(false)
            .build();
        
        // Get response from LLM
        ChatCompletionResponse response = llmClientService.createChatCompletion(request);
        
        if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
            return response.getChoices().get(0).getMessage().getContent();
        } else {
            // Fallback to existing summary if LLM fails
            return dialogueState.getSummaryShort() != null ? 
                dialogueState.getSummaryShort() : "Session summary unavailable";
        }
    }

    /**
     * Build the summarization prompt for the LLM.
     *
     * @param dialogueState The dialogue state
     * @return Formatted prompt
     */
    private String buildSummarizationPrompt(DialogueState dialogueState) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Please provide a concise summary of the following conversation session.\n\n");
        
        if (dialogueState.getSummaryShort() != null) {
            prompt.append("Current short summary: ").append(dialogueState.getSummaryShort()).append("\n\n");
        }
        
        if (dialogueState.getSummaryBullets() != null) {
            prompt.append("Current bullet points: ").append(dialogueState.getSummaryBullets()).append("\n\n");
        }
        
        if (dialogueState.getTopics() != null) {
            prompt.append("Topics discussed: ").append(dialogueState.getTopics()).append("\n\n");
        }
        
        prompt.append("Session metadata:\n");
        prompt.append("- Turn count: ").append(dialogueState.getTurnCount()).append("\n");
        prompt.append("- Cumulative tokens: ").append(dialogueState.getCumulativeTokens()).append("\n");
        prompt.append("- Last updated: ").append(dialogueState.getLastUpdatedAt()).append("\n\n");
        
        prompt.append("Please provide a comprehensive summary that captures the key points, decisions, and outcomes of this conversation.");
        
        return prompt.toString();
    }

    /**
     * Create a session memory knowledge object.
     *
     * @param sessionId The session identifier
     * @param tenantId The tenant identifier
     * @param dialogueState The dialogue state
     * @param summary The generated summary
     * @return Created knowledge object
     */
    private KnowledgeObject createSessionMemory(String sessionId, String tenantId, 
                                              DialogueState dialogueState, String summary) {
        KnowledgeObject sessionMemory = new KnowledgeObject();
        sessionMemory.setId(UUID.randomUUID().toString());
        sessionMemory.setTenantId(tenantId);
        sessionMemory.setType(KnowledgeObjectType.SESSION_MEMORY);
        sessionMemory.setSessionId(sessionId);
        sessionMemory.setUserId(dialogueState.getUserId());
        sessionMemory.setParentId(null);
        
        // Create metadata with session information
        String metadata = String.format("""
            {
                "summary": "%s",
                "turn_count": %d,
                "cumulative_tokens": %d,
                "topics": %s,
                "created_at": "%s"
            }
            """, 
            summary.replace("\"", "\\\""),
            dialogueState.getTurnCount(),
            dialogueState.getCumulativeTokens(),
            dialogueState.getTopics() != null ? dialogueState.getTopics() : "[]",
            LocalDateTime.now()
        );
        
        sessionMemory.setMetadata(metadata);
        sessionMemory.setOriginalTokens(tokenCountingService.countTokens(summary, "gpt-3.5-turbo"));
        sessionMemory.setArchived(false);
        sessionMemory.setCreatedAt(LocalDateTime.now());
        
        return knowledgeObjectRepository.save(sessionMemory);
    }

    /**
     * Update the dialogue state with the new summary.
     *
     * @param dialogueState The dialogue state to update
     * @param summary The new summary
     */
    private void updateDialogueState(DialogueState dialogueState, String summary) {
        // Update the short summary (keep it under 250 tokens)
        String shortSummary = summary.length() > 250 ? 
            summary.substring(0, 247) + "..." : summary;
        
        dialogueState.setSummaryShort(shortSummary);
        dialogueState.setLastUpdatedAt(LocalDateTime.now());
        
        dialogueStateRepository.save(dialogueState);
    }

    /**
     * Batch summarize multiple sessions for a tenant.
     *
     * @param tenantId The tenant identifier
     * @param sessionIds List of session IDs to summarize
     * @return Summary of batch operation
     */
    public Map<String, Object> batchSummarizeSessions(String tenantId, List<String> sessionIds) {
        int successCount = 0;
        int failureCount = 0;
        
        for (String sessionId : sessionIds) {
            try {
                Map<String, Object> result = summarizeSession(sessionId, tenantId);
                if (!result.containsKey("error")) {
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (Exception e) {
                failureCount++;
            }
        }
        
        return Map.of(
            "status", "completed",
            "total_sessions", sessionIds.size(),
            "successful", successCount,
            "failed", failureCount
        );
    }
}

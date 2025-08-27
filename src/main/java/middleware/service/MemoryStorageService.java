package middleware.service;

import middleware.dto.ChatMessage;
import middleware.service.MemoryExtractionService.MemoryExtraction;

import java.util.List;
import java.util.Map;

/**
 * Service interface for coordinating memory storage operations.
 * Handles the complete flow from conversation to stored knowledge objects.
 */
public interface MemoryStorageService {
    
    /**
     * Process a complete conversation turn and store all extracted memories.
     * 
     * @param tenantId The tenant identifier
     * @param sessionId The session identifier
     * @param userId The user identifier
     * @param userMessage The user message
     * @param assistantMessage The assistant response
     * @param context Additional context information
     * @return Map containing the created knowledge object IDs and metadata
     */
    Map<String, Object> processConversationTurn(String tenantId, String sessionId, String userId,
                                              String userMessage, String assistantMessage, 
                                              Map<String, Object> context);
    
    /**
     * Extract and store memories from a conversation turn.
     * 
     * @param tenantId The tenant identifier
     * @param sessionId The session identifier
     * @param userId The user identifier
     * @param userMessage The user message
     * @param assistantMessage The assistant response
     * @param context Additional context information
     * @return List of created knowledge object IDs
     */
    List<String> extractAndStoreMemories(String tenantId, String sessionId, String userId,
                                       String userMessage, String assistantMessage, 
                                       Map<String, Object> context);
    
    /**
     * Store conversation turns as knowledge objects.
     * 
     * @param tenantId The tenant identifier
     * @param sessionId The session identifier
     * @param userId The user identifier
     * @param userMessage The user message
     * @param assistantMessage The assistant response
     * @param metadata Additional metadata
     * @return Map containing user turn ID and assistant turn ID
     */
    Map<String, String> storeConversationTurns(String tenantId, String sessionId, String userId,
                                              String userMessage, String assistantMessage, 
                                              Map<String, Object> metadata);
    
    /**
     * Process memory extraction results and store as knowledge objects.
     * 
     * @param tenantId The tenant identifier
     * @param sessionId The session identifier
     * @param userId The user identifier
     * @param memoryExtraction The extracted memory information
     * @return List of created knowledge object IDs
     */
    List<String> processMemoryExtraction(String tenantId, String sessionId, String userId,
                                       MemoryExtraction memoryExtraction);
    
    /**
     * Trigger relationship discovery for newly created knowledge objects.
     * 
     * @param tenantId The tenant identifier
     * @param knowledgeObjectIds List of knowledge object IDs to process
     * @return Number of relationships discovered
     */
    int discoverRelationships(String tenantId, List<String> knowledgeObjectIds);
    
    /**
     * Check if session should be summarized based on turn count or token usage.
     * 
     * @param tenantId The tenant identifier
     * @param sessionId The session identifier
     * @return true if session should be summarized
     */
    boolean shouldSummarizeSession(String tenantId, String sessionId);
    
    /**
     * Create session memory and trigger summarization if needed.
     * 
     * @param tenantId The tenant identifier
     * @param sessionId The session identifier
     * @param userId The user identifier
     * @return Session memory knowledge object ID if created, null otherwise
     */
    String createSessionMemoryIfNeeded(String tenantId, String sessionId, String userId);
}

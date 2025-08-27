package middleware.service;

import middleware.model.KnowledgeObject;
import middleware.model.ContentVariant;
import middleware.service.MemoryExtractionService.MemoryExtraction;
import middleware.service.MemoryExtractionService.Fact;
import middleware.service.MemoryExtractionService.Entity;
import middleware.service.MemoryExtractionService.Task;

import java.util.List;
import java.util.Map;

/**
 * Service interface for managing knowledge objects and content variants.
 * Handles automatic creation and storage of knowledge objects from conversations,
 * memory extraction, and session management.
 */
public interface KnowledgeObjectService {
    
    /**
     * Create a TURN type knowledge object for a user message.
     * 
     * @param tenantId The tenant identifier
     * @param sessionId The session identifier
     * @param userId The user identifier
     * @param userMessage The user message content
     * @param metadata Additional metadata for the turn
     * @return Created knowledge object
     */
    KnowledgeObject createUserTurn(String tenantId, String sessionId, String userId, 
                                  String userMessage, Map<String, Object> metadata);
    
    /**
     * Create a TURN type knowledge object for an LLM response.
     * 
     * @param tenantId The tenant identifier
     * @param sessionId The session identifier
     * @param userId The user identifier
     * @param assistantMessage The assistant response content
     * @param metadata Additional metadata for the turn
     * @return Created knowledge object
     */
    KnowledgeObject createAssistantTurn(String tenantId, String sessionId, String userId, 
                                      String assistantMessage, Map<String, Object> metadata);
    
    /**
     * Create EXTRACTED_FACT type knowledge objects from memory extraction.
     * 
     * @param tenantId The tenant identifier
     * @param sessionId The session identifier
     * @param userId The user identifier
     * @param memoryExtraction The extracted memory information
     * @return List of created knowledge objects
     */
    List<KnowledgeObject> createExtractedFacts(String tenantId, String sessionId, String userId, 
                                              MemoryExtraction memoryExtraction);
    
    /**
     * Create a SESSION_MEMORY type knowledge object for session summarization.
     * 
     * @param tenantId The tenant identifier
     * @param sessionId The session identifier
     * @param userId The user identifier
     * @param summary The session summary
     * @param metadata Additional metadata for the session
     * @return Created knowledge object
     */
    KnowledgeObject createSessionMemory(String tenantId, String sessionId, String userId, 
                                      String summary, Map<String, Object> metadata);
    
    /**
     * Create content variants for a knowledge object.
     * 
     * @param knowledgeObject The knowledge object
     * @param rawContent The raw content
     * @param shortContent The short content variant
     * @param bulletFacts The bullet facts variant
     * @return List of created content variants
     */
    List<ContentVariant> createContentVariants(KnowledgeObject knowledgeObject, 
                                              String rawContent, String shortContent, 
                                              String bulletFacts);
    
    /**
     * Store a knowledge object with its content variants and embeddings.
     * 
     * @param knowledgeObject The knowledge object to store
     * @param contentVariants The content variants to store
     * @return The stored knowledge object with generated ID
     */
    KnowledgeObject storeKnowledgeObject(KnowledgeObject knowledgeObject, 
                                        List<ContentVariant> contentVariants);
    
    /**
     * Find knowledge objects by session and type.
     * 
     * @param tenantId The tenant identifier
     * @param sessionId The session identifier
     * @param type The knowledge object type
     * @return List of knowledge objects
     */
    List<KnowledgeObject> findBySessionAndType(String tenantId, String sessionId, 
                                              middleware.model.KnowledgeObjectType type);
    
    /**
     * Update knowledge object metadata.
     * 
     * @param knowledgeObjectId The knowledge object ID
     * @param tenantId The tenant identifier
     * @param metadata The updated metadata
     * @return Updated knowledge object
     */
    KnowledgeObject updateMetadata(String knowledgeObjectId, String tenantId, 
                                 Map<String, Object> metadata);
    
    /**
     * Archive a knowledge object.
     * 
     * @param knowledgeObjectId The knowledge object ID
     * @param tenantId The tenant identifier
     * @return true if successfully archived
     */
    boolean archiveKnowledgeObject(String knowledgeObjectId, String tenantId);
}

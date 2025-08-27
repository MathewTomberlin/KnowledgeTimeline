package middleware.service.impl;

import middleware.model.KnowledgeObject;
import middleware.model.KnowledgeObjectType;
import middleware.model.ContentVariant;
import middleware.service.MemoryStorageService;
import middleware.service.MemoryExtractionService;
import middleware.service.MemoryExtractionService.MemoryExtraction;
import middleware.service.KnowledgeObjectService;
import middleware.service.DialogueStateService;
import middleware.service.impl.RelationshipDiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of MemoryStorageService for coordinating memory storage operations.
 * Handles the complete flow from conversation to stored knowledge objects.
 */
@Service
public class MemoryStorageServiceImpl implements MemoryStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryStorageServiceImpl.class);
    
    private final MemoryExtractionService memoryExtractionService;
    private final KnowledgeObjectService knowledgeObjectService;
    private final DialogueStateService dialogueStateService;
    private final RelationshipDiscoveryService relationshipDiscoveryService;
    
    @Value("${knowledge.session.summarize.turn-count:10}")
    private int sessionSummarizeTurnCount;
    
    @Value("${knowledge.session.summarize.token-threshold:3000}")
    private int sessionSummarizeTokenThreshold;
    
    @Autowired
    public MemoryStorageServiceImpl(MemoryExtractionService memoryExtractionService,
                                 KnowledgeObjectService knowledgeObjectService,
                                 DialogueStateService dialogueStateService,
                                 RelationshipDiscoveryService relationshipDiscoveryService) {
        this.memoryExtractionService = memoryExtractionService;
        this.knowledgeObjectService = knowledgeObjectService;
        this.dialogueStateService = dialogueStateService;
        this.relationshipDiscoveryService = relationshipDiscoveryService;
    }
    
    @Override
    @Transactional
    public Map<String, Object> processConversationTurn(String tenantId, String sessionId, String userId,
                                                     String userMessage, String assistantMessage, 
                                                     Map<String, Object> context) {
        logger.debug("Processing conversation turn for tenant: {}, session: {}, user: {}", 
                    tenantId, sessionId, userId);
        
        try {
            // Store conversation turns
            Map<String, String> turnIds = storeConversationTurns(tenantId, sessionId, userId, 
                                                               userMessage, assistantMessage, context);
            
            // Extract and store memories
            List<String> memoryIds = extractAndStoreMemories(tenantId, sessionId, userId, 
                                                           userMessage, assistantMessage, context);
            
            // Update dialogue state
            dialogueStateService.updateDialogueState(sessionId, userMessage, assistantMessage, 0);
            
            // Check if session should be summarized
            String sessionMemoryId = createSessionMemoryIfNeeded(tenantId, sessionId, userId);
            
            // Trigger relationship discovery asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    List<String> allIds = new ArrayList<>();
                    allIds.addAll(turnIds.values());
                    allIds.addAll(memoryIds);
                    if (sessionMemoryId != null) {
                        allIds.add(sessionMemoryId);
                    }
                    
                    if (!allIds.isEmpty()) {
                        discoverRelationships(tenantId, allIds);
                    }
                } catch (Exception e) {
                    logger.error("Error in async relationship discovery for tenant: {}, session: {}", 
                               tenantId, sessionId, e);
                }
            });
            
            // Build result
            Map<String, Object> result = new HashMap<>();
            result.put("user_turn_id", turnIds.get("user_turn_id"));
            result.put("assistant_turn_id", turnIds.get("assistant_turn_id"));
            result.put("memory_ids", memoryIds);
            result.put("session_memory_id", sessionMemoryId);
            result.put("processed_at", LocalDateTime.now().toString());
            
            logger.debug("Successfully processed conversation turn. Created {} memory objects", memoryIds.size());
            return result;
            
        } catch (Exception e) {
            logger.error("Error processing conversation turn for tenant: {}, session: {}", 
                        tenantId, sessionId, e);
            throw new RuntimeException("Failed to process conversation turn", e);
        }
    }
    
    @Override
    @Transactional
    public List<String> extractAndStoreMemories(String tenantId, String sessionId, String userId,
                                              String userMessage, String assistantMessage, 
                                              Map<String, Object> context) {
        logger.debug("Extracting and storing memories for tenant: {}, session: {}", tenantId, sessionId);
        
        try {
            // Extract memories using the memory extraction service
            MemoryExtraction memoryExtraction = memoryExtractionService.extractMemory(
                userMessage, assistantMessage, context);
            
            // Process and store the extracted memories
            return processMemoryExtraction(tenantId, sessionId, userId, memoryExtraction);
            
        } catch (Exception e) {
            logger.error("Error extracting and storing memories for tenant: {}, session: {}", 
                        tenantId, sessionId, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    @Transactional
    public Map<String, String> storeConversationTurns(String tenantId, String sessionId, String userId,
                                                     String userMessage, String assistantMessage, 
                                                     Map<String, Object> metadata) {
        logger.debug("Storing conversation turns for tenant: {}, session: {}", tenantId, sessionId);
        
        Map<String, String> result = new HashMap<>();
        
        try {
            // Create user turn knowledge object
            logger.error("Creating user turn knowledge object");
            KnowledgeObject userTurn = knowledgeObjectService.createUserTurn(tenantId, sessionId, userId,
                                                                          userMessage, metadata);
            logger.error("Created user turn with ID: {}", userTurn.getId());
            
            // Create content variants for user turn
            List<ContentVariant> userVariants = knowledgeObjectService.createContentVariants(
                userTurn, userMessage, null, null);
            
            // Store user turn
            KnowledgeObject savedUserTurn = knowledgeObjectService.storeKnowledgeObject(userTurn, userVariants);
            result.put("user_turn_id", savedUserTurn.getId().toString());
            
            // Create assistant turn knowledge object
            KnowledgeObject assistantTurn = knowledgeObjectService.createAssistantTurn(tenantId, sessionId, userId, 
                                                                                    assistantMessage, metadata);
            
            // Create content variants for assistant turn
            List<ContentVariant> assistantVariants = knowledgeObjectService.createContentVariants(
                assistantTurn, assistantMessage, null, null);
            
            // Store assistant turn
            KnowledgeObject savedAssistantTurn = knowledgeObjectService.storeKnowledgeObject(assistantTurn, assistantVariants);
            result.put("assistant_turn_id", savedAssistantTurn.getId().toString());
            
            logger.debug("Stored conversation turns. User turn: {}, Assistant turn: {}",
                        savedUserTurn.getId().toString(), savedAssistantTurn.getId().toString());
            
        } catch (Exception e) {
            logger.error("Error storing conversation turns for tenant: {}, session: {}", 
                        tenantId, sessionId, e);
            throw new RuntimeException("Failed to store conversation turns", e);
        }
        
        return result;
    }
    
    @Override
    @Transactional
    public List<String> processMemoryExtraction(String tenantId, String sessionId, String userId,
                                              MemoryExtraction memoryExtraction) {
        logger.debug("Processing memory extraction for tenant: {}, session: {}", tenantId, sessionId);
        
        List<String> createdIds = new ArrayList<>();
        
        try {
            // Create knowledge objects for extracted facts
            List<KnowledgeObject> factObjects = knowledgeObjectService.createExtractedFacts(
                tenantId, sessionId, userId, memoryExtraction);
            
            // Store each fact object with content variants
            for (KnowledgeObject factObject : factObjects) {
                // Create content variants (using the fact content as raw content)
                String content = extractContentFromFact(factObject);
                List<ContentVariant> variants = knowledgeObjectService.createContentVariants(
                    factObject, content, null, null);
                
                // Store the knowledge object
                KnowledgeObject savedObject = knowledgeObjectService.storeKnowledgeObject(factObject, variants);
                createdIds.add(savedObject.getId().toString());
            }
            
            logger.debug("Processed memory extraction. Created {} knowledge objects", createdIds.size());
            
        } catch (Exception e) {
            logger.error("Error processing memory extraction for tenant: {}, session: {}", 
                        tenantId, sessionId, e);
            throw new RuntimeException("Failed to process memory extraction", e);
        }
        
        return createdIds;
    }
    
    @Override
    @Async
    public int discoverRelationships(String tenantId, List<String> knowledgeObjectIds) {
        logger.debug("Discovering relationships for tenant: {}, {} knowledge objects", 
                    tenantId, knowledgeObjectIds.size());
        
        try {
            // Use the existing relationship discovery service for each object
            int totalRelationships = 0;
            for (String objectId : knowledgeObjectIds) {
                totalRelationships += relationshipDiscoveryService.discoverRelationshipsForObject(objectId, tenantId);
            }
            return totalRelationships;
            
        } catch (Exception e) {
            logger.error("Error discovering relationships for tenant: {}", tenantId, e);
            return 0;
        }
    }
    
    @Override
    public boolean shouldSummarizeSession(String tenantId, String sessionId) {
        try {
            // For now, we'll use a simple token threshold check
            // This can be enhanced later with more sophisticated logic
            return false;
            
        } catch (Exception e) {
            logger.error("Error checking if session should be summarized for tenant: {}, session: {}", 
                        tenantId, sessionId, e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public String createSessionMemoryIfNeeded(String tenantId, String sessionId, String userId) {
        if (!shouldSummarizeSession(tenantId, sessionId)) {
            return null;
        }
        
        logger.debug("Creating session memory for tenant: {}, session: {}", tenantId, sessionId);
        
        try {
            // For now, create a simple session memory
            // This can be enhanced later with actual dialogue state data
            String summary = "Session summary placeholder";
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("created_at", LocalDateTime.now().toString());
            
            KnowledgeObject sessionMemory = knowledgeObjectService.createSessionMemory(
                tenantId, sessionId, userId, summary, metadata);
            
            // Create content variants
            List<ContentVariant> variants = knowledgeObjectService.createContentVariants(
                sessionMemory, summary, null, null);
            
            // Store session memory
            KnowledgeObject savedSessionMemory = knowledgeObjectService.storeKnowledgeObject(sessionMemory, variants);
            
            logger.debug("Created session memory: {}", savedSessionMemory.getId().toString());
            return savedSessionMemory.getId().toString();
            
        } catch (Exception e) {
            logger.error("Error creating session memory for tenant: {}, session: {}", 
                        tenantId, sessionId, e);
            return null;
        }
    }
    
    /**
     * Extract content from a fact knowledge object for content variant creation.
     */
    private String extractContentFromFact(KnowledgeObject factObject) {
        // For extracted facts, the content is stored in the metadata
        // We'll create a summary from the metadata for now
        StringBuilder content = new StringBuilder();

        if (factObject.getType() == KnowledgeObjectType.EXTRACTED_FACT) {
            // Try to extract content from metadata
            try {
                String metadata = factObject.getMetadata();
                if (metadata != null && !metadata.equals("{}")) {
                    // Simple extraction - look for source information
                    if (metadata.contains("conversation")) {
                        content.append("Extracted fact from conversation");
                    } else {
                        content.append("Extracted fact");
                    }
                }
            } catch (Exception e) {
                logger.debug("Could not extract content from fact metadata", e);
            }
        }

        if (content.length() == 0) {
            content.append("Extracted fact content");
        }

        return content.toString();
    }
}

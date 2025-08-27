package middleware.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import middleware.model.KnowledgeObject;
import middleware.model.ContentVariant;
import middleware.model.KnowledgeObjectType;
import middleware.model.ContentVariantType;
import middleware.repository.KnowledgeObjectRepository;
import middleware.repository.ContentVariantRepository;
import middleware.service.KnowledgeObjectService;
import middleware.service.EmbeddingService;
import middleware.service.TokenCountingService;
import middleware.service.MemoryExtractionService.MemoryExtraction;
import middleware.service.MemoryExtractionService.Fact;
import middleware.service.MemoryExtractionService.Entity;
import middleware.service.MemoryExtractionService.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of KnowledgeObjectService for managing knowledge objects and content variants.
 * Handles automatic creation and storage of knowledge objects from conversations,
 * memory extraction, and session management.
 */
@Service
public class KnowledgeObjectServiceImpl implements KnowledgeObjectService {
    
    private static final Logger logger = LoggerFactory.getLogger(KnowledgeObjectServiceImpl.class);
    
    private final KnowledgeObjectRepository knowledgeObjectRepository;
    private final ContentVariantRepository contentVariantRepository;
    private final EmbeddingService embeddingService;
    private final TokenCountingService tokenCountingService;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public KnowledgeObjectServiceImpl(KnowledgeObjectRepository knowledgeObjectRepository,
                                   ContentVariantRepository contentVariantRepository,
                                   EmbeddingService embeddingService,
                                   TokenCountingService tokenCountingService) {
        this.knowledgeObjectRepository = knowledgeObjectRepository;
        this.contentVariantRepository = contentVariantRepository;
        this.embeddingService = embeddingService;
        this.tokenCountingService = tokenCountingService;
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    @Transactional
    public KnowledgeObject createUserTurn(String tenantId, String sessionId, String userId, 
                                        String userMessage, Map<String, Object> metadata) {
        logger.debug("Creating user turn knowledge object for session: {}, user: {}", sessionId, userId);
        
        KnowledgeObject knowledgeObject = new KnowledgeObject();
        knowledgeObject.setTenantId(tenantId);
        knowledgeObject.setType(KnowledgeObjectType.TURN);
        knowledgeObject.setSessionId(sessionId);
        knowledgeObject.setUserId(userId);
        knowledgeObject.setParentId(null);
        knowledgeObject.setTags("[]"); // Empty JSON array as string
        knowledgeObject.setMetadata(serializeMetadata(metadata));
        knowledgeObject.setArchived(false);
        knowledgeObject.setCreatedAt(LocalDateTime.now());
        knowledgeObject.setOriginalTokens(tokenCountingService.countTokens(userMessage, "gpt-3.5-turbo"));
        
        return knowledgeObject;
    }
    
    @Override
    @Transactional
    public KnowledgeObject createAssistantTurn(String tenantId, String sessionId, String userId, 
                                            String assistantMessage, Map<String, Object> metadata) {
        logger.debug("Creating assistant turn knowledge object for session: {}, user: {}", sessionId, userId);
        
        KnowledgeObject knowledgeObject = new KnowledgeObject();
        knowledgeObject.setTenantId(tenantId);
        knowledgeObject.setType(KnowledgeObjectType.TURN);
        knowledgeObject.setSessionId(sessionId);
        knowledgeObject.setUserId(userId);
        knowledgeObject.setParentId(null);
        knowledgeObject.setTags("[]"); // Empty JSON array as string
        knowledgeObject.setMetadata(serializeMetadata(metadata));
        knowledgeObject.setArchived(false);
        knowledgeObject.setCreatedAt(LocalDateTime.now());
        knowledgeObject.setOriginalTokens(tokenCountingService.countTokens(assistantMessage, "gpt-3.5-turbo"));
        
        return knowledgeObject;
    }
    
    @Override
    @Transactional
    public List<KnowledgeObject> createExtractedFacts(String tenantId, String sessionId, String userId, 
                                                    MemoryExtraction memoryExtraction) {
        logger.debug("Creating extracted fact knowledge objects for session: {}, user: {}", sessionId, userId);
        
        List<KnowledgeObject> knowledgeObjects = new ArrayList<>();
        
        // Create knowledge objects for facts
        if (memoryExtraction.getFacts() != null) {
            for (Fact fact : memoryExtraction.getFacts()) {
                KnowledgeObject factObject = new KnowledgeObject();
                factObject.setTenantId(tenantId);
                factObject.setType(KnowledgeObjectType.EXTRACTED_FACT);
                factObject.setSessionId(sessionId);
                factObject.setUserId(userId);
                factObject.setParentId(null);
                factObject.setTags(serializeTags(fact.getTags()));
                
                Map<String, Object> factMetadata = new HashMap<>();
                factMetadata.put("source", fact.getSource());
                factMetadata.put("confidence", fact.getConfidence());
                factMetadata.put("extraction_method", "llm");
                factMetadata.put("extracted_at", LocalDateTime.now().toString());
                
                factObject.setMetadata(serializeMetadata(factMetadata));
                factObject.setArchived(false);
                factObject.setCreatedAt(LocalDateTime.now());
                factObject.setOriginalTokens(tokenCountingService.countTokens(fact.getContent(), "gpt-3.5-turbo"));
                
                knowledgeObjects.add(factObject);
            }
        }
        
        // Create knowledge objects for entities
        if (memoryExtraction.getEntities() != null) {
            for (Entity entity : memoryExtraction.getEntities()) {
                KnowledgeObject entityObject = new KnowledgeObject();
                entityObject.setTenantId(tenantId);
                entityObject.setType(KnowledgeObjectType.EXTRACTED_FACT);
                entityObject.setSessionId(sessionId);
                entityObject.setUserId(userId);
                entityObject.setParentId(null);
                entityObject.setTags("[]"); // Entities don't have tags in the current model
                
                Map<String, Object> entityMetadata = new HashMap<>();
                entityMetadata.put("entity_type", entity.getType());
                entityMetadata.put("description", entity.getDescription());
                entityMetadata.put("confidence", entity.getConfidence());
                entityMetadata.put("attributes", entity.getAttributes());
                entityMetadata.put("extraction_method", "llm");
                entityMetadata.put("extracted_at", LocalDateTime.now().toString());
                
                entityObject.setMetadata(serializeMetadata(entityMetadata));
                entityObject.setArchived(false);
                entityObject.setCreatedAt(LocalDateTime.now());
                entityObject.setOriginalTokens(tokenCountingService.countTokens(entity.getName(), "gpt-3.5-turbo"));
                
                knowledgeObjects.add(entityObject);
            }
        }
        
        // Create knowledge objects for tasks
        if (memoryExtraction.getTasks() != null) {
            for (Task task : memoryExtraction.getTasks()) {
                KnowledgeObject taskObject = new KnowledgeObject();
                taskObject.setTenantId(tenantId);
                taskObject.setType(KnowledgeObjectType.EXTRACTED_FACT);
                taskObject.setSessionId(sessionId);
                taskObject.setUserId(userId);
                taskObject.setParentId(null);
                taskObject.setTags("[]"); // Tasks don't have tags in the current model
                
                Map<String, Object> taskMetadata = new HashMap<>();
                taskMetadata.put("status", task.getStatus());
                taskMetadata.put("priority", task.getPriority());
                taskMetadata.put("extraction_method", "llm");
                taskMetadata.put("extracted_at", LocalDateTime.now().toString());
                
                taskObject.setMetadata(serializeMetadata(taskMetadata));
                taskObject.setArchived(false);
                taskObject.setCreatedAt(LocalDateTime.now());
                taskObject.setOriginalTokens(tokenCountingService.countTokens(task.getDescription(), "gpt-3.5-turbo"));
                
                knowledgeObjects.add(taskObject);
            }
        }
        
        logger.debug("Created {} extracted fact knowledge objects", knowledgeObjects.size());
        return knowledgeObjects;
    }
    
    @Override
    @Transactional
    public KnowledgeObject createSessionMemory(String tenantId, String sessionId, String userId, 
                                            String summary, Map<String, Object> metadata) {
        logger.debug("Creating session memory knowledge object for session: {}, user: {}", sessionId, userId);
        
        KnowledgeObject knowledgeObject = new KnowledgeObject();
        knowledgeObject.setTenantId(tenantId);
        knowledgeObject.setType(KnowledgeObjectType.SESSION_MEMORY);
        knowledgeObject.setSessionId(sessionId);
        knowledgeObject.setUserId(userId);
        knowledgeObject.setParentId(null);
        knowledgeObject.setTags("[]"); // Empty JSON array as string
        knowledgeObject.setMetadata(serializeMetadata(metadata));
        knowledgeObject.setArchived(false);
        knowledgeObject.setCreatedAt(LocalDateTime.now());
        knowledgeObject.setOriginalTokens(tokenCountingService.countTokens(summary, "gpt-3.5-turbo"));
        
        return knowledgeObject;
    }
    
    @Override
    @Transactional
    public List<ContentVariant> createContentVariants(KnowledgeObject knowledgeObject, 
                                                    String rawContent, String shortContent, 
                                                    String bulletFacts) {
        logger.debug("Creating content variants for knowledge object: {}", knowledgeObject.getId());
        
        List<ContentVariant> variants = new ArrayList<>();
        
        // Create RAW variant
        if (rawContent != null && !rawContent.trim().isEmpty()) {
            ContentVariant rawVariant = new ContentVariant();
            rawVariant.setKnowledgeObjectId(knowledgeObject.getId());
            rawVariant.setVariant(ContentVariantType.RAW);
            rawVariant.setContent(rawContent);
            rawVariant.setTokens(tokenCountingService.countTokens(rawContent, "gpt-3.5-turbo"));
            rawVariant.setCreatedAt(LocalDateTime.now());
            variants.add(rawVariant);
        }
        
        // Create SHORT variant
        if (shortContent != null && !shortContent.trim().isEmpty()) {
            ContentVariant shortVariant = new ContentVariant();
            shortVariant.setKnowledgeObjectId(knowledgeObject.getId());
            shortVariant.setVariant(ContentVariantType.SHORT);
            shortVariant.setContent(shortContent);
            shortVariant.setTokens(tokenCountingService.countTokens(shortContent, "gpt-3.5-turbo"));
            shortVariant.setCreatedAt(LocalDateTime.now());
            variants.add(shortVariant);
        }
        
        // Create BULLET_FACTS variant
        if (bulletFacts != null && !bulletFacts.trim().isEmpty()) {
            ContentVariant bulletVariant = new ContentVariant();
            bulletVariant.setKnowledgeObjectId(knowledgeObject.getId());
            bulletVariant.setVariant(ContentVariantType.BULLET_FACTS);
            bulletVariant.setContent(bulletFacts);
            bulletVariant.setTokens(tokenCountingService.countTokens(bulletFacts, "gpt-3.5-turbo"));
            bulletVariant.setCreatedAt(LocalDateTime.now());
            variants.add(bulletVariant);
        }
        
        logger.debug("Created {} content variants", variants.size());
        return variants;
    }
    
    @Override
    @Transactional
    public KnowledgeObject storeKnowledgeObject(KnowledgeObject knowledgeObject, 
                                              List<ContentVariant> contentVariants) {
        logger.debug("Storing knowledge object with {} content variants", contentVariants.size());
        
        // Save the knowledge object first
        KnowledgeObject savedKnowledgeObject = knowledgeObjectRepository.save(knowledgeObject);
        
        // Save content variants
        for (ContentVariant variant : contentVariants) {
            variant.setKnowledgeObjectId(savedKnowledgeObject.getId());
            contentVariantRepository.save(variant);
        }
        
        logger.debug("Successfully stored knowledge object: {} with {} content variants", 
                    savedKnowledgeObject.getId(), contentVariants.size());
        
        return savedKnowledgeObject;
    }
    
    @Override
    public List<KnowledgeObject> findBySessionAndType(String tenantId, String sessionId, 
                                                    KnowledgeObjectType type) {
        logger.debug("Finding knowledge objects by session: {}, type: {}", sessionId, type);
        
        return knowledgeObjectRepository.findBySessionIdAndTenantIdAndType(sessionId, tenantId, type);
    }
    
    @Override
    @Transactional
    public KnowledgeObject updateMetadata(String knowledgeObjectId, String tenantId, 
                                       Map<String, Object> metadata) {
        logger.debug("Updating metadata for knowledge object: {}", knowledgeObjectId);
        
        Optional<KnowledgeObject> optional = knowledgeObjectRepository.findByIdAndTenantId(knowledgeObjectId, tenantId);
        if (optional.isEmpty()) {
            throw new IllegalArgumentException("Knowledge object not found: " + knowledgeObjectId);
        }
        
        KnowledgeObject knowledgeObject = optional.get();
        knowledgeObject.setMetadata(serializeMetadata(metadata));
        
        return knowledgeObjectRepository.save(knowledgeObject);
    }
    
    @Override
    @Transactional
    public boolean archiveKnowledgeObject(String knowledgeObjectId, String tenantId) {
        logger.debug("Archiving knowledge object: {}", knowledgeObjectId);
        
        Optional<KnowledgeObject> optional = knowledgeObjectRepository.findByIdAndTenantId(knowledgeObjectId, tenantId);
        if (optional.isEmpty()) {
            return false;
        }
        
        KnowledgeObject knowledgeObject = optional.get();
        knowledgeObject.setArchived(true);
        knowledgeObjectRepository.save(knowledgeObject);
        
        return true;
    }
    
    /**
     * Serialize metadata map to JSON string.
     */
    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }
        
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize metadata, using empty object", e);
            return "{}";
        }
    }
    
    /**
     * Serialize tags list to JSON string.
     */
    private String serializeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "[]";
        }
        
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize tags, using empty array", e);
            return "[]";
        }
    }
}

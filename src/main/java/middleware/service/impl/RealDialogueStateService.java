package middleware.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import middleware.model.DialogueState;
import middleware.repository.DialogueStateRepository;
import middleware.service.DialogueStateService;
import middleware.service.LLMClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Real implementation of DialogueStateService for managing conversation state.
 * Handles session persistence, summarization, and state management.
 */
@Service
@Profile({"local", "docker"})  // Only active for production profiles
public class RealDialogueStateService implements DialogueStateService {
    
    private static final Logger logger = LoggerFactory.getLogger(RealDialogueStateService.class);
    
    private final DialogueStateRepository dialogueStateRepository;
    private final LLMClientService llmClientService;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public RealDialogueStateService(DialogueStateRepository dialogueStateRepository,
                                   LLMClientService llmClientService,
                                   ObjectMapper objectMapper) {
        this.dialogueStateRepository = dialogueStateRepository;
        this.llmClientService = llmClientService;
        this.objectMapper = objectMapper;
    }
    
    @Override
    @Transactional(readOnly = true)
    public DialogueState getOrCreateDialogueState(String tenantId, String sessionId, String userId) {
        logger.debug("Getting or creating dialogue state for tenant: {}, session: {}, user: {}", 
                    tenantId, sessionId, userId);
        
        Optional<middleware.model.DialogueState> existingState = 
            dialogueStateRepository.findBySessionIdAndTenantId(sessionId, tenantId);
        
        if (existingState.isPresent()) {
            middleware.model.DialogueState entity = existingState.get();
            return convertToDto(entity);
        }
        
        // Create new dialogue state
        middleware.model.DialogueState newState = new middleware.model.DialogueState(tenantId, sessionId, userId);
        middleware.model.DialogueState savedState = dialogueStateRepository.save(newState);
        
        logger.info("Created new dialogue state for session: {}", sessionId);
        return convertToDto(savedState);
    }
    
    @Override
    @Transactional
    public void updateDialogueState(String sessionId, String userMessage, String assistantMessage, int knowledgeTokens) {
        logger.debug("Updating dialogue state for session: {} with {} knowledge tokens", sessionId, knowledgeTokens);
        
        // Note: This method needs tenantId to find the session, but we don't have it in the interface
        // For now, we'll need to modify the interface or use a different approach
        // This is a limitation of the current interface design
        logger.warn("Cannot update dialogue state without tenantId - interface needs to be updated");
        return;
    }
    
    @Override
    @Transactional
    public DialogueState summarizeDialogueState(String sessionId) {
        logger.debug("Summarizing dialogue state for session: {}", sessionId);
        
        // Note: This method needs tenantId to find the session, but we don't have it in the interface
        // For now, we'll need to modify the interface or use a different approach
        logger.warn("Cannot summarize dialogue state without tenantId - interface needs to be updated");
        return null;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<DialogueState> getRecentDialogueStates(String tenantId, String userId, int limit) {
        logger.debug("Getting recent dialogue states for tenant: {}, user: {}, limit: {}", tenantId, userId, limit);
        
        // Use the available repository method with Pageable
        org.springframework.data.domain.PageRequest pageRequest = 
            org.springframework.data.domain.PageRequest.of(0, limit);
        List<middleware.model.DialogueState> entities = 
            dialogueStateRepository.findRecentByUserIdAndTenantId(userId, tenantId, pageRequest);
        
        return entities.stream()
                      .map(this::convertToDto)
                      .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public int cleanupOldDialogueStates(String tenantId, int olderThanDays) {
        logger.info("Cleaning up dialogue states older than {} days for tenant: {}", olderThanDays, tenantId);
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(olderThanDays);
        int deletedCount = dialogueStateRepository.deleteOldByTenantId(tenantId, cutoffDate);
        
        logger.info("Deleted {} old dialogue states for tenant: {}", deletedCount, tenantId);
        return deletedCount;
    }
    
    private DialogueState convertToDto(middleware.model.DialogueState entity) {
        DialogueState dto = new DialogueState(entity.getId(), entity.getTenantId(), 
                                            entity.getSessionId(), entity.getUserId());
        
        dto.setSummaryShort(entity.getSummaryShort());
        dto.setSummaryBullets(entity.getSummaryBullets());
        dto.setCumulativeTokens(entity.getCumulativeTokens() != null ? entity.getCumulativeTokens() : 0);
        dto.setLastUpdatedAt(entity.getLastUpdatedAt());
        
        // Parse topics from JSON
        if (entity.getTopics() != null) {
            try {
                List<String> topics = objectMapper.readValue(entity.getTopics(), new TypeReference<List<String>>() {});
                dto.setTopics(topics);
            } catch (JsonProcessingException e) {
                logger.warn("Error parsing topics JSON for session: {}", entity.getSessionId(), e);
                dto.setTopics(Collections.emptyList());
            }
        } else {
            dto.setTopics(Collections.emptyList());
        }
        
        // Parse metadata from JSON
        if (entity.getMetadata() != null) {
            try {
                Map<String, Object> metadata = objectMapper.readValue(entity.getMetadata(), 
                                                                     new TypeReference<Map<String, Object>>() {});
                dto.setMetadata(metadata);
            } catch (JsonProcessingException e) {
                logger.warn("Error parsing metadata JSON for session: {}", entity.getSessionId(), e);
                dto.setMetadata(Collections.emptyMap());
            }
        }
        
        return dto;
    }
    
    private void updateMetadata(middleware.model.DialogueState state, String userMessage, String assistantMessage) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            
            if (state.getMetadata() != null) {
                metadata = objectMapper.readValue(state.getMetadata(), new TypeReference<Map<String, Object>>() {});
            }
            
            // Add conversation history (keep last 10 turns)
            @SuppressWarnings("unchecked")
            List<Map<String, String>> history = (List<Map<String, String>>) metadata.getOrDefault("conversation_history", new ArrayList<>());
            
            Map<String, String> turn = new HashMap<>();
            turn.put("user", userMessage);
            turn.put("assistant", assistantMessage);
            turn.put("timestamp", LocalDateTime.now().toString());
            
            history.add(turn);
            
            // Keep only last 10 turns
            if (history.size() > 10) {
                history = history.subList(history.size() - 10, history.size());
            }
            
            metadata.put("conversation_history", history);
            metadata.put("last_updated", LocalDateTime.now().toString());
            
            state.setMetadata(objectMapper.writeValueAsString(metadata));
            
        } catch (JsonProcessingException e) {
            logger.warn("Error updating metadata for session: {}", state.getSessionId(), e);
        }
    }
    
    private String buildSummaryPrompt(middleware.model.DialogueState state) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Please summarize the following conversation and extract key topics.\n\n");
        
        // Add conversation history if available
        if (state.getMetadata() != null) {
            try {
                Map<String, Object> metadata = objectMapper.readValue(state.getMetadata(), 
                                                                     new TypeReference<Map<String, Object>>() {});
                @SuppressWarnings("unchecked")
                List<Map<String, String>> history = (List<Map<String, String>>) metadata.get("conversation_history");
                
                if (history != null) {
                    prompt.append("Conversation:\n");
                    for (Map<String, String> turn : history) {
                        prompt.append("User: ").append(turn.get("user")).append("\n");
                        prompt.append("Assistant: ").append(turn.get("assistant")).append("\n\n");
                    }
                }
            } catch (JsonProcessingException e) {
                logger.warn("Error parsing conversation history for summary", e);
            }
        }
        
        prompt.append("Please provide:\n");
        prompt.append("1. A short summary (max 250 characters)\n");
        prompt.append("2. Key bullet points (max 120 characters)\n");
        prompt.append("3. Main topics discussed\n\n");
        prompt.append("Format your response as JSON:\n");
        prompt.append("{\n");
        prompt.append("  \"short_summary\": \"brief summary\",\n");
        prompt.append("  \"bullet_summary\": \"key points\",\n");
        prompt.append("  \"topics\": [\"topic1\", \"topic2\"]\n");
        prompt.append("}");
        
        return prompt.toString();
    }
    
    private SummaryResult parseSummaryResponse(String response) {
        try {
            // Try to parse as JSON first
            Map<String, Object> json = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {});
            
            String shortSummary = (String) json.getOrDefault("short_summary", "");
            String bulletSummary = (String) json.getOrDefault("bullet_summary", "");
            @SuppressWarnings("unchecked")
            List<String> topics = (List<String>) json.getOrDefault("topics", Collections.emptyList());
            
            return new SummaryResult(shortSummary, bulletSummary, topics);
            
        } catch (Exception e) {
            logger.warn("Error parsing LLM summary response, using fallback", e);
            
            // Fallback: extract summary from text
            String[] lines = response.split("\n");
            String shortSummary = lines.length > 0 ? lines[0].substring(0, Math.min(lines[0].length(), 250)) : "";
            String bulletSummary = lines.length > 1 ? lines[1].substring(0, Math.min(lines[1].length(), 120)) : "";
            
            return new SummaryResult(shortSummary, bulletSummary, Collections.emptyList());
        }
    }
    
    private static class SummaryResult {
        final String shortSummary;
        final String bulletSummary;
        final List<String> topics;
        
        SummaryResult(String shortSummary, String bulletSummary, List<String> topics) {
            this.shortSummary = shortSummary;
            this.bulletSummary = bulletSummary;
            this.topics = topics;
        }
    }
}

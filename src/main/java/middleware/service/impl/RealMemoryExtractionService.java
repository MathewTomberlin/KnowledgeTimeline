package middleware.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import middleware.service.MemoryExtractionService;
import middleware.service.MemoryExtractionService.*;
import middleware.service.LLMClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Real implementation of MemoryExtractionService using LLM for structured extraction.
 * Extracts facts, entities, and tasks from conversations using JSON schema validation.
 */
@Service
@Profile({"local", "docker"})  // Only active for production profiles
public class RealMemoryExtractionService implements MemoryExtractionService {

    private static final Logger logger = LoggerFactory.getLogger(RealMemoryExtractionService.class);
    
    @Autowired
    private LLMClientService llmClientService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, MemoryExtraction> extractions = new ConcurrentHashMap<>();
    
    // JSON schema for extraction
    private static final String EXTRACTION_SCHEMA = """
        {
            "type": "object",
            "properties": {
                "facts": {
                    "type": "array",
                    "items": {
                        "type": "object",
                        "properties": {
                            "content": {"type": "string"},
                            "source": {"type": "string"},
                            "confidence": {"type": "number", "minimum": 0, "maximum": 1},
                            "tags": {"type": "array", "items": {"type": "string"}}
                        },
                        "required": ["content", "source", "confidence"]
                    }
                },
                "entities": {
                    "type": "array",
                    "items": {
                        "type": "object",
                        "properties": {
                            "name": {"type": "string"},
                            "type": {"type": "string"},
                            "description": {"type": "string"},
                            "confidence": {"type": "number", "minimum": 0, "maximum": 1},
                            "attributes": {"type": "object"}
                        },
                        "required": ["name", "type", "description", "confidence"]
                    }
                },
                "tasks": {
                    "type": "array",
                    "items": {
                        "type": "object",
                        "properties": {
                            "description": {"type": "string"},
                            "status": {"type": "string"},
                            "priority": {"type": "string"},
                            "assignee": {"type": "string"},
                            "dueDate": {"type": "string", "format": "date-time"}
                        },
                        "required": ["description", "status"]
                    }
                },
                "confidence": {"type": "number", "minimum": 0, "maximum": 1}
            },
            "required": ["facts", "entities", "tasks", "confidence"]
        }
    """;

    @Override
    public MemoryExtraction extractMemory(String userMessage, String assistantMessage, Map<String, Object> context) {
        logger.debug("Extracting memory from conversation turn");
        
        try {
            // Build prompt for LLM extraction
            String prompt = buildExtractionPrompt(userMessage, assistantMessage, context);
            
            // Get LLM response using chat completion
            middleware.dto.ChatCompletionRequest request = new middleware.dto.ChatCompletionRequest.Builder()
                .model("gpt-3.5-turbo")
                .messages(List.of(new middleware.dto.ChatMessage("user", prompt)))
                .temperature(0.1)
                .maxTokens(1000)
                .build();
            
            middleware.dto.ChatCompletionResponse response = llmClientService.createChatCompletion(request);
            String responseText = response.getChoices().get(0).getMessage().getContent();
            
            // Parse and validate JSON response
            MemoryExtraction extraction = parseExtractionResponse(responseText);
            
            // Validate and deduplicate facts
            extraction.setFacts(validateAndDeduplicateFacts(extraction.getFacts()));
            
            logger.debug("Successfully extracted {} facts, {} entities, {} tasks", 
                        extraction.getFacts().size(), 
                        extraction.getEntities().size(), 
                        extraction.getTasks().size());
            
            return extraction;
            
        } catch (Exception e) {
            logger.error("Error extracting memory from conversation", e);
            return createFallbackExtraction(userMessage, assistantMessage);
        }
    }

    @Override
    public List<MemoryExtraction> extractMemoryBatch(List<ConversationTurn> conversations) {
        logger.info("Extracting memory from batch of {} conversations", conversations.size());
        
        return conversations.parallelStream()
            .map(turn -> extractMemory(turn.getUserMessage(), turn.getAssistantMessage(), turn.getContext()))
            .collect(Collectors.toList());
    }

    @Override
    public List<Fact> validateAndDeduplicateFacts(List<Fact> facts) {
        if (facts == null || facts.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Remove null or invalid facts
        List<Fact> validFacts = facts.stream()
            .filter(fact -> fact != null && fact.getContent() != null && !fact.getContent().trim().isEmpty())
            .filter(fact -> fact.getConfidence() >= 0.0 && fact.getConfidence() <= 1.0)
            .collect(Collectors.toList());
        
        // Deduplicate based on content similarity
        Map<String, Fact> uniqueFacts = new LinkedHashMap<>();
        for (Fact fact : validFacts) {
            String normalizedContent = normalizeFactContent(fact.getContent());
            if (!uniqueFacts.containsKey(normalizedContent)) {
                uniqueFacts.put(normalizedContent, fact);
            } else {
                // Keep the fact with higher confidence
                Fact existing = uniqueFacts.get(normalizedContent);
                if (fact.getConfidence() > existing.getConfidence()) {
                    uniqueFacts.put(normalizedContent, fact);
                }
            }
        }
        
        logger.debug("Deduplicated {} facts to {} unique facts", facts.size(), uniqueFacts.size());
        return new ArrayList<>(uniqueFacts.values());
    }

    @Override
    public void storeMemoryExtraction(String tenantId, String sessionId, MemoryExtraction memoryExtraction) {
        String key = tenantId + ":" + sessionId;
        extractions.put(key, memoryExtraction);
        logger.debug("Stored memory extraction for tenant: {}, session: {}", tenantId, sessionId);
    }
    
    /**
     * Build extraction prompt for LLM.
     */
    private String buildExtractionPrompt(String userMessage, String assistantMessage, Map<String, Object> context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Extract structured information from the following conversation turn. ");
        prompt.append("Return a JSON object with the following structure:\n\n");
        prompt.append(EXTRACTION_SCHEMA);
        prompt.append("\n\nConversation:\n");
        prompt.append("User: ").append(userMessage).append("\n");
        prompt.append("Assistant: ").append(assistantMessage).append("\n");
        
        if (context != null && !context.isEmpty()) {
            prompt.append("\nContext: ").append(context.toString()).append("\n");
        }
        
        prompt.append("\nExtraction Instructions:\n");
        prompt.append("1. Extract factual statements as 'facts'\n");
        prompt.append("2. Identify named entities, concepts, and topics as 'entities'\n");
        prompt.append("3. Identify actionable tasks or follow-ups as 'tasks'\n");
        prompt.append("4. Assign confidence scores (0.0-1.0) based on certainty\n");
        prompt.append("5. Return only valid JSON matching the schema\n\n");
        prompt.append("JSON Response:");
        
        return prompt.toString();
    }
    
    /**
     * Parse and validate LLM response.
     */
    private MemoryExtraction parseExtractionResponse(String response) {
        try {
            // Clean up response to extract JSON
            String jsonResponse = extractJsonFromResponse(response);
            
            // Parse JSON
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            
            // Extract facts
            List<Fact> facts = new ArrayList<>();
            JsonNode factsNode = rootNode.get("facts");
            if (factsNode != null && factsNode.isArray()) {
                for (JsonNode factNode : factsNode) {
                    Fact fact = new Fact(
                        factNode.get("content").asText(),
                        factNode.get("source").asText(),
                        factNode.get("confidence").asDouble()
                    );
                    if (factNode.has("tags") && factNode.get("tags").isArray()) {
                        List<String> tags = new ArrayList<>();
                        for (JsonNode tagNode : factNode.get("tags")) {
                            tags.add(tagNode.asText());
                        }
                        fact.setTags(tags);
                    }
                    facts.add(fact);
                }
            }
            
            // Extract entities
            List<Entity> entities = new ArrayList<>();
            JsonNode entitiesNode = rootNode.get("entities");
            if (entitiesNode != null && entitiesNode.isArray()) {
                for (JsonNode entityNode : entitiesNode) {
                    Entity entity = new Entity(
                        entityNode.get("name").asText(),
                        entityNode.get("type").asText(),
                        entityNode.get("description").asText(),
                        entityNode.get("confidence").asDouble()
                    );
                    if (entityNode.has("attributes") && entityNode.get("attributes").isObject()) {
                        Map<String, Object> attributes = new HashMap<>();
                        entityNode.get("attributes").fieldNames().forEachRemaining(key -> 
                            attributes.put(key, entityNode.get("attributes").get(key).asText())
                        );
                        entity.setAttributes(attributes);
                    }
                    entities.add(entity);
                }
            }
            
            // Extract tasks
            List<Task> tasks = new ArrayList<>();
            JsonNode tasksNode = rootNode.get("tasks");
            if (tasksNode != null && tasksNode.isArray()) {
                for (JsonNode taskNode : tasksNode) {
                    Task task = new Task(
                        taskNode.get("description").asText(),
                        taskNode.get("status").asText()
                    );
                    if (taskNode.has("priority")) {
                        task.setPriority(taskNode.get("priority").asText());
                    }
                    if (taskNode.has("assignee")) {
                        task.setAssignee(taskNode.get("assignee").asText());
                    }
                    tasks.add(task);
                }
            }
            
            // Get overall confidence
            double confidence = rootNode.has("confidence") ? rootNode.get("confidence").asDouble() : 0.5;
            
            return new MemoryExtraction(facts, entities, tasks, confidence, Map.of(
                "extraction_method", "llm",
                "response_length", response.length()
            ));
            
        } catch (JsonProcessingException e) {
            logger.error("Error parsing LLM response as JSON", e);
            throw new RuntimeException("Failed to parse extraction response", e);
        }
    }
    
    /**
     * Extract JSON from LLM response.
     */
    private String extractJsonFromResponse(String response) {
        // Try to find JSON object in the response
        int startIndex = response.indexOf('{');
        int endIndex = response.lastIndexOf('}');
        
        if (startIndex >= 0 && endIndex > startIndex) {
            return response.substring(startIndex, endIndex + 1);
        }
        
        // If no JSON found, return the original response
        return response;
    }
    
    /**
     * Normalize fact content for deduplication.
     */
    private String normalizeFactContent(String content) {
        if (content == null) {
            return "";
        }
        
        return content.toLowerCase()
            .replaceAll("\\s+", " ")
            .trim();
    }
    
    /**
     * Create fallback extraction when LLM fails.
     */
    private MemoryExtraction createFallbackExtraction(String userMessage, String assistantMessage) {
        List<Fact> facts = List.of(
            new Fact("Conversation occurred between user and assistant", "fallback", 0.5),
            new Fact("User message: " + userMessage.substring(0, Math.min(100, userMessage.length())), "fallback", 0.3)
        );
        
        List<Entity> entities = List.of(
            new Entity("conversation", "dialogue", "A conversation turn", 0.5)
        );
        
        List<Task> tasks = new ArrayList<>();
        
        return new MemoryExtraction(
            facts,
            entities,
            tasks,
            0.3,
            Map.of("extraction_method", "fallback", "error", "llm_extraction_failed")
        );
    }
}

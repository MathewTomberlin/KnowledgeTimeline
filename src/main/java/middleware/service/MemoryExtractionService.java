package middleware.service;

import java.util.List;
import java.util.Map;

/**
 * Service interface for extracting structured information from conversations.
 * Extracts facts, entities, and tasks from dialogue content.
 */
public interface MemoryExtractionService {
    
    /**
     * Extract structured information from a conversation turn.
     * 
     * @param userMessage The user message
     * @param assistantMessage The assistant response
     * @param context Additional context information
     * @return Extracted memory information
     */
    MemoryExtraction extractMemory(String userMessage, String assistantMessage, Map<String, Object> context);
    
    /**
     * Extract structured information from a batch of conversations.
     * 
     * @param conversations List of conversation turns
     * @return List of extracted memory information
     */
    List<MemoryExtraction> extractMemoryBatch(List<ConversationTurn> conversations);
    
    /**
     * Validate and deduplicate extracted facts.
     * 
     * @param facts List of facts to validate
     * @return Validated and deduplicated facts
     */
    List<Fact> validateAndDeduplicateFacts(List<Fact> facts);
    
    /**
     * Store extracted memory information.
     * 
     * @param tenantId The tenant ID
     * @param sessionId The session ID
     * @param memoryExtraction The extracted memory
     */
    void storeMemoryExtraction(String tenantId, String sessionId, MemoryExtraction memoryExtraction);
    
    /**
     * DTO for memory extraction results.
     */
    class MemoryExtraction {
        private List<Fact> facts;
        private List<Entity> entities;
        private List<Task> tasks;
        private double confidence;
        private Map<String, Object> metadata;
        
        public MemoryExtraction(List<Fact> facts, List<Entity> entities, List<Task> tasks, 
                              double confidence, Map<String, Object> metadata) {
            this.facts = facts;
            this.entities = entities;
            this.tasks = tasks;
            this.confidence = confidence;
            this.metadata = metadata;
        }
        
        // Getters and Setters
        public List<Fact> getFacts() {
            return facts;
        }
        
        public void setFacts(List<Fact> facts) {
            this.facts = facts;
        }
        
        public List<Entity> getEntities() {
            return entities;
        }
        
        public void setEntities(List<Entity> entities) {
            this.entities = entities;
        }
        
        public List<Task> getTasks() {
            return tasks;
        }
        
        public void setTasks(List<Task> tasks) {
            this.tasks = tasks;
        }
        
        public double getConfidence() {
            return confidence;
        }
        
        public void setConfidence(double confidence) {
            this.confidence = confidence;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        
        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }
    
    /**
     * DTO for extracted facts.
     */
    class Fact {
        private String id;
        private String content;
        private String source;
        private double confidence;
        private List<String> tags;
        private Map<String, Object> metadata;
        
        public Fact(String content, String source, double confidence) {
            this.content = content;
            this.source = source;
            this.confidence = confidence;
        }
        
        // Getters and Setters
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public String getSource() {
            return source;
        }
        
        public void setSource(String source) {
            this.source = source;
        }
        
        public double getConfidence() {
            return confidence;
        }
        
        public void setConfidence(double confidence) {
            this.confidence = confidence;
        }
        
        public List<String> getTags() {
            return tags;
        }
        
        public void setTags(List<String> tags) {
            this.tags = tags;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        
        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }
    
    /**
     * DTO for extracted entities.
     */
    class Entity {
        private String id;
        private String name;
        private String type;
        private String description;
        private double confidence;
        private Map<String, Object> attributes;
        
        public Entity(String name, String type, String description, double confidence) {
            this.name = name;
            this.type = type;
            this.description = description;
            this.confidence = confidence;
        }
        
        // Getters and Setters
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public double getConfidence() {
            return confidence;
        }
        
        public void setConfidence(double confidence) {
            this.confidence = confidence;
        }
        
        public Map<String, Object> getAttributes() {
            return attributes;
        }
        
        public void setAttributes(Map<String, Object> attributes) {
            this.attributes = attributes;
        }
    }
    
    /**
     * DTO for extracted tasks.
     */
    class Task {
        private String id;
        private String description;
        private String status;
        private String assignee;
        private String priority;
        private java.time.LocalDateTime dueDate;
        private Map<String, Object> metadata;
        
        public Task(String description, String status) {
            this.description = description;
            this.status = status;
        }
        
        // Getters and Setters
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public String getAssignee() {
            return assignee;
        }
        
        public void setAssignee(String assignee) {
            this.assignee = assignee;
        }
        
        public String getPriority() {
            return priority;
        }
        
        public void setPriority(String priority) {
            this.priority = priority;
        }
        
        public java.time.LocalDateTime getDueDate() {
            return dueDate;
        }
        
        public void setDueDate(java.time.LocalDateTime dueDate) {
            this.dueDate = dueDate;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        
        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }
    
    /**
     * DTO for conversation turns.
     */
    class ConversationTurn {
        private String userMessage;
        private String assistantMessage;
        private Map<String, Object> context;
        
        public ConversationTurn(String userMessage, String assistantMessage, Map<String, Object> context) {
            this.userMessage = userMessage;
            this.assistantMessage = assistantMessage;
            this.context = context;
        }
        
        // Getters and Setters
        public String getUserMessage() {
            return userMessage;
        }
        
        public void setUserMessage(String userMessage) {
            this.userMessage = userMessage;
        }
        
        public String getAssistantMessage() {
            return assistantMessage;
        }
        
        public void setAssistantMessage(String assistantMessage) {
            this.assistantMessage = assistantMessage;
        }
        
        public Map<String, Object> getContext() {
            return context;
        }
        
        public void setContext(Map<String, Object> context) {
            this.context = context;
        }
    }
}

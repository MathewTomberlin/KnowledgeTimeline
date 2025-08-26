package middleware.service;

import java.util.List;
import java.util.Map;

/**
 * Service interface for building knowledge-aware context for LLM prompts.
 * Implements summaries-first approach with MMR packing and token budget management.
 */
public interface ContextBuilderService {
    
    /**
     * Build enhanced context for a chat completion request.
     * 
     * @param tenantId The tenant ID
     * @param sessionId The session ID
     * @param userPrompt The user's prompt
     * @param knowledgeContext The knowledge context configuration
     * @return The enhanced context string
     */
    String buildContext(String tenantId, String sessionId, String userPrompt, 
                       Map<String, Object> knowledgeContext);
    
    /**
     * Get knowledge objects that are relevant to the query.
     * 
     * @param tenantId The tenant ID
     * @param query The search query
     * @param maxResults Maximum number of results
     * @param filters Optional filters
     * @return List of relevant knowledge objects
     */
    List<KnowledgeObject> getRelevantKnowledge(String tenantId, String query, 
                                              int maxResults, Map<String, Object> filters);
    
    /**
     * Pack knowledge objects into context within token budget.
     * 
     * @param knowledgeObjects List of knowledge objects
     * @param tokenBudget Maximum tokens allowed
     * @param diversity Diversity parameter for MMR
     * @return Packed context string
     */
    String packKnowledgeIntoContext(List<KnowledgeObject> knowledgeObjects, 
                                  int tokenBudget, double diversity);
    
    /**
     * Get the current token budget for the tenant.
     * 
     * @param tenantId The tenant ID
     * @return The token budget
     */
    int getTokenBudget(String tenantId);
    
    /**
     * DTO for knowledge objects used in context building.
     */
    class KnowledgeObject {
        private String id;
        private String type;
        private String title;
        private String content;
        private String variantType;
        private double relevanceScore;
        private Map<String, Object> metadata;
        
        public KnowledgeObject(String id, String type, String title, String content,
                             String variantType, double relevanceScore, Map<String, Object> metadata) {
            this.id = id;
            this.type = type;
            this.title = title;
            this.content = content;
            this.variantType = variantType;
            this.relevanceScore = relevanceScore;
            this.metadata = metadata;
        }
        
        // Getters and Setters
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public String getVariantType() {
            return variantType;
        }
        
        public void setVariantType(String variantType) {
            this.variantType = variantType;
        }
        
        public double getRelevanceScore() {
            return relevanceScore;
        }
        
        public void setRelevanceScore(double relevanceScore) {
            this.relevanceScore = relevanceScore;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        
        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }
}

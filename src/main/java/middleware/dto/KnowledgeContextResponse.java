package middleware.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO for knowledge context information in responses.
 */
public class KnowledgeContextResponse {
    
    @JsonProperty("objects_used")
    private List<KnowledgeObjectUsed> objectsUsed;
    
    @JsonProperty("relationships_found")
    private Integer relationshipsFound;
    
    // Constructors
    public KnowledgeContextResponse() {}
    
    public KnowledgeContextResponse(List<KnowledgeObjectUsed> objectsUsed, Integer relationshipsFound) {
        this.objectsUsed = objectsUsed;
        this.relationshipsFound = relationshipsFound;
    }
    
    // Getters and Setters
    public List<KnowledgeObjectUsed> getObjectsUsed() {
        return objectsUsed;
    }
    
    public void setObjectsUsed(List<KnowledgeObjectUsed> objectsUsed) {
        this.objectsUsed = objectsUsed;
    }
    
    public Integer getRelationshipsFound() {
        return relationshipsFound;
    }
    
    public void setRelationshipsFound(Integer relationshipsFound) {
        this.relationshipsFound = relationshipsFound;
    }
    
    /**
     * Inner class for knowledge objects used in context.
     */
    public static class KnowledgeObjectUsed {
        
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("title")
        private String title;
        
        @JsonProperty("relevance_score")
        private Double relevanceScore;
        
        // Constructors
        public KnowledgeObjectUsed() {}
        
        public KnowledgeObjectUsed(String id, String type, String title, Double relevanceScore) {
            this.id = id;
            this.type = type;
            this.title = title;
            this.relevanceScore = relevanceScore;
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
        
        public Double getRelevanceScore() {
            return relevanceScore;
        }
        
        public void setRelevanceScore(Double relevanceScore) {
            this.relevanceScore = relevanceScore;
        }
    }
}

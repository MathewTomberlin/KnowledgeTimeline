package middleware.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for knowledge context configuration.
 */
public class KnowledgeContext {
    
    @JsonProperty("include_recent")
    private Boolean includeRecent = true;
    
    @JsonProperty("include_related")
    private Boolean includeRelated = true;
    
    @JsonProperty("max_context_objects")
    private Integer maxContextObjects = 10;
    
    @JsonProperty("similarity_threshold")
    private Double similarityThreshold = 0.8;
    
    // Constructors
    public KnowledgeContext() {}
    
    public KnowledgeContext(Boolean includeRecent, Boolean includeRelated, 
                          Integer maxContextObjects, Double similarityThreshold) {
        this.includeRecent = includeRecent;
        this.includeRelated = includeRelated;
        this.maxContextObjects = maxContextObjects;
        this.similarityThreshold = similarityThreshold;
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Boolean includeRecent = true;
        private Boolean includeRelated = true;
        private Integer maxContextObjects = 10;
        private Double similarityThreshold = 0.8;
        
        public Builder includeRecent(Boolean includeRecent) {
            this.includeRecent = includeRecent;
            return this;
        }
        
        public Builder includeRelated(Boolean includeRelated) {
            this.includeRelated = includeRelated;
            return this;
        }
        
        public Builder maxContextObjects(Integer maxContextObjects) {
            this.maxContextObjects = maxContextObjects;
            return this;
        }
        
        public Builder similarityThreshold(Double similarityThreshold) {
            this.similarityThreshold = similarityThreshold;
            return this;
        }
        
        public KnowledgeContext build() {
            return new KnowledgeContext(includeRecent, includeRelated, 
                                      maxContextObjects, similarityThreshold);
        }
    }
    
    // Getters and Setters
    public Boolean getIncludeRecent() {
        return includeRecent;
    }
    
    public void setIncludeRecent(Boolean includeRecent) {
        this.includeRecent = includeRecent;
    }
    
    public Boolean getIncludeRelated() {
        return includeRelated;
    }
    
    public void setIncludeRelated(Boolean includeRelated) {
        this.includeRelated = includeRelated;
    }
    
    public Integer getMaxContextObjects() {
        return maxContextObjects;
    }
    
    public void setMaxContextObjects(Integer maxContextObjects) {
        this.maxContextObjects = maxContextObjects;
    }
    
    public Double getSimilarityThreshold() {
        return similarityThreshold;
    }
    
    public void setSimilarityThreshold(Double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }
}

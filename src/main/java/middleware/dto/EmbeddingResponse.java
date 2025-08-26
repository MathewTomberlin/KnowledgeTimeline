package middleware.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO for embedding generation responses.
 */
public class EmbeddingResponse {
    
    @JsonProperty("object")
    private String object = "list";
    
    @JsonProperty("data")
    private List<EmbeddingData> data;
    
    @JsonProperty("model")
    private String model;
    
    @JsonProperty("usage")
    private TokenUsage usage;
    
    @JsonProperty("knowledge_object_id")
    private String knowledgeObjectId;
    
    // Constructors
    public EmbeddingResponse() {}
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String object = "list";
        private List<EmbeddingData> data;
        private String model;
        private TokenUsage usage;
        private String knowledgeObjectId;
        
        public Builder object(String object) {
            this.object = object;
            return this;
        }
        
        public Builder data(List<EmbeddingData> data) {
            this.data = data;
            return this;
        }
        
        public Builder model(String model) {
            this.model = model;
            return this;
        }
        
        public Builder usage(TokenUsage usage) {
            this.usage = usage;
            return this;
        }
        
        public Builder knowledgeObjectId(String knowledgeObjectId) {
            this.knowledgeObjectId = knowledgeObjectId;
            return this;
        }
        
        public EmbeddingResponse build() {
            EmbeddingResponse response = new EmbeddingResponse();
            response.object = this.object;
            response.data = this.data;
            response.model = this.model;
            response.usage = this.usage;
            response.knowledgeObjectId = this.knowledgeObjectId;
            return response;
        }
    }
    
    // Getters and Setters
    public String getObject() {
        return object;
    }
    
    public void setObject(String object) {
        this.object = object;
    }
    
    public List<EmbeddingData> getData() {
        return data;
    }
    
    public void setData(List<EmbeddingData> data) {
        this.data = data;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public TokenUsage getUsage() {
        return usage;
    }
    
    public void setUsage(TokenUsage usage) {
        this.usage = usage;
    }
    
    public String getKnowledgeObjectId() {
        return knowledgeObjectId;
    }
    
    public void setKnowledgeObjectId(String knowledgeObjectId) {
        this.knowledgeObjectId = knowledgeObjectId;
    }
    
    /**
     * Inner class for embedding data.
     */
    public static class EmbeddingData {
        
        @JsonProperty("object")
        private String object = "embedding";
        
        @JsonProperty("embedding")
        private List<Double> embedding;
        
        @JsonProperty("index")
        private Integer index;
        
        // Constructors
        public EmbeddingData() {}
        
        public EmbeddingData(List<Double> embedding, Integer index) {
            this.embedding = embedding;
            this.index = index;
        }
        
        // Getters and Setters
        public String getObject() {
            return object;
        }
        
        public void setObject(String object) {
            this.object = object;
        }
        
        public List<Double> getEmbedding() {
            return embedding;
        }
        
        public void setEmbedding(List<Double> embedding) {
            this.embedding = embedding;
        }
        
        public Integer getIndex() {
            return index;
        }
        
        public void setIndex(Integer index) {
            this.index = index;
        }
    }
}

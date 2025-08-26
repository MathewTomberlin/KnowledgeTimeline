package middleware.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for embedding generation requests.
 */
public class EmbeddingRequest {
    
    @JsonProperty("model")
    private String model;
    
    @JsonProperty("input")
    private String input;
    
    @JsonProperty("store_knowledge")
    private Boolean storeKnowledge = false;
    
    @JsonProperty("metadata")
    private Object metadata;
    
    // Constructors
    public EmbeddingRequest() {}
    
    public EmbeddingRequest(String model, String input) {
        this.model = model;
        this.input = input;
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String model;
        private String input;
        private Boolean storeKnowledge = false;
        private Object metadata;
        
        public Builder model(String model) {
            this.model = model;
            return this;
        }
        
        public Builder input(String input) {
            this.input = input;
            return this;
        }
        
        public Builder storeKnowledge(Boolean storeKnowledge) {
            this.storeKnowledge = storeKnowledge;
            return this;
        }
        
        public Builder metadata(Object metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public EmbeddingRequest build() {
            EmbeddingRequest request = new EmbeddingRequest();
            request.model = this.model;
            request.input = this.input;
            request.storeKnowledge = this.storeKnowledge;
            request.metadata = this.metadata;
            return request;
        }
    }
    
    // Getters and Setters
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public String getInput() {
        return input;
    }
    
    public void setInput(String input) {
        this.input = input;
    }
    
    public Boolean getStoreKnowledge() {
        return storeKnowledge;
    }
    
    public void setStoreKnowledge(Boolean storeKnowledge) {
        this.storeKnowledge = storeKnowledge;
    }
    
    public Object getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Object metadata) {
        this.metadata = metadata;
    }
}

package middleware.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO for chat completion requests, following OpenAI's API format.
 */
public class ChatCompletionRequest {
    
    @JsonProperty("model")
    private String model;
    
    @JsonProperty("messages")
    private List<ChatMessage> messages;
    
    @JsonProperty("temperature")
    private Double temperature;
    
    @JsonProperty("max_tokens")
    private Integer maxTokens;
    
    @JsonProperty("stream")
    private Boolean stream;
    
    @JsonProperty("knowledge_context")
    private KnowledgeContext knowledgeContext;
    
    // Constructors
    public ChatCompletionRequest() {}
    
    public ChatCompletionRequest(String model, List<ChatMessage> messages) {
        this.model = model;
        this.messages = messages;
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String model;
        private List<ChatMessage> messages;
        private Double temperature = 0.7;
        private Integer maxTokens = 1000;
        private Boolean stream = false;
        private KnowledgeContext knowledgeContext;
        
        public Builder model(String model) {
            this.model = model;
            return this;
        }
        
        public Builder messages(List<ChatMessage> messages) {
            this.messages = messages;
            return this;
        }
        
        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }
        
        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }
        
        public Builder stream(Boolean stream) {
            this.stream = stream;
            return this;
        }
        
        public Builder knowledgeContext(KnowledgeContext knowledgeContext) {
            this.knowledgeContext = knowledgeContext;
            return this;
        }
        
        public ChatCompletionRequest build() {
            ChatCompletionRequest request = new ChatCompletionRequest();
            request.model = this.model;
            request.messages = this.messages;
            request.temperature = this.temperature;
            request.maxTokens = this.maxTokens;
            request.stream = this.stream;
            request.knowledgeContext = this.knowledgeContext;
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
    
    public List<ChatMessage> getMessages() {
        return messages;
    }
    
    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }
    
    public Double getTemperature() {
        return temperature;
    }
    
    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }
    
    public Integer getMaxTokens() {
        return maxTokens;
    }
    
    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }
    
    public Boolean getStream() {
        return stream;
    }
    
    public void setStream(Boolean stream) {
        this.stream = stream;
    }
    
    public KnowledgeContext getKnowledgeContext() {
        return knowledgeContext;
    }
    
    public void setKnowledgeContext(KnowledgeContext knowledgeContext) {
        this.knowledgeContext = knowledgeContext;
    }
}

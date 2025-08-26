package middleware.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO for chat completion responses, following OpenAI's API format.
 */
public class ChatCompletionResponse {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("object")
    private String object = "chat.completion";
    
    @JsonProperty("created")
    private Long created;
    
    @JsonProperty("model")
    private String model;
    
    @JsonProperty("choices")
    private List<ChatChoice> choices;
    
    @JsonProperty("usage")
    private TokenUsage usage;
    
    @JsonProperty("knowledge_context")
    private KnowledgeContextResponse knowledgeContext;
    
    // Constructors
    public ChatCompletionResponse() {
        this.created = System.currentTimeMillis() / 1000;
    }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String id;
        private String object = "chat.completion";
        private Long created = System.currentTimeMillis() / 1000;
        private String model;
        private List<ChatChoice> choices;
        private TokenUsage usage;
        private KnowledgeContextResponse knowledgeContext;
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder object(String object) {
            this.object = object;
            return this;
        }
        
        public Builder created(Long created) {
            this.created = created;
            return this;
        }
        
        public Builder model(String model) {
            this.model = model;
            return this;
        }
        
        public Builder choices(List<ChatChoice> choices) {
            this.choices = choices;
            return this;
        }
        
        public Builder usage(TokenUsage usage) {
            this.usage = usage;
            return this;
        }
        
        public Builder knowledgeContext(KnowledgeContextResponse knowledgeContext) {
            this.knowledgeContext = knowledgeContext;
            return this;
        }
        
        public ChatCompletionResponse build() {
            ChatCompletionResponse response = new ChatCompletionResponse();
            response.id = this.id;
            response.object = this.object;
            response.created = this.created;
            response.model = this.model;
            response.choices = this.choices;
            response.usage = this.usage;
            response.knowledgeContext = this.knowledgeContext;
            return response;
        }
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getObject() {
        return object;
    }
    
    public void setObject(String object) {
        this.object = object;
    }
    
    public Long getCreated() {
        return created;
    }
    
    public void setCreated(Long created) {
        this.created = created;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public List<ChatChoice> getChoices() {
        return choices;
    }
    
    public void setChoices(List<ChatChoice> choices) {
        this.choices = choices;
    }
    
    public TokenUsage getUsage() {
        return usage;
    }
    
    public void setUsage(TokenUsage usage) {
        this.usage = usage;
    }
    
    public KnowledgeContextResponse getKnowledgeContext() {
        return knowledgeContext;
    }
    
    public void setKnowledgeContext(KnowledgeContextResponse knowledgeContext) {
        this.knowledgeContext = knowledgeContext;
    }
}

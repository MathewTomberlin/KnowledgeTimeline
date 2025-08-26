package middleware.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for chat completion choices.
 */
public class ChatChoice {
    
    @JsonProperty("index")
    private Integer index;
    
    @JsonProperty("message")
    private ChatMessage message;
    
    @JsonProperty("finish_reason")
    private String finishReason;
    
    // Constructors
    public ChatChoice() {}
    
    public ChatChoice(Integer index, ChatMessage message, String finishReason) {
        this.index = index;
        this.message = message;
        this.finishReason = finishReason;
    }
    
    // Getters and Setters
    public Integer getIndex() {
        return index;
    }
    
    public void setIndex(Integer index) {
        this.index = index;
    }
    
    public ChatMessage getMessage() {
        return message;
    }
    
    public void setMessage(ChatMessage message) {
        this.message = message;
    }
    
    public String getFinishReason() {
        return finishReason;
    }
    
    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }
}

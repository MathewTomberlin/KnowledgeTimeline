package middleware.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "usage_logs")
public class UsageLog {
    
    @Id
    private String id;
    
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    
    @Column(name = "user_id")
    private String userId;
    
    @Column(name = "session_id")
    private String sessionId;
    
    @Column(name = "request_id")
    private String requestId;
    
    @Column(name = "knowledge_tokens_used")
    private Integer knowledgeTokensUsed;
    
    @Column(name = "llm_input_tokens")
    private Integer llmInputTokens;
    
    @Column(name = "llm_output_tokens")
    private Integer llmOutputTokens;
    
    @Column(name = "cost_estimate", precision = 10, scale = 6)
    private BigDecimal costEstimate;
    
    @Column(name = "model")
    private String model;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
    
    // Constructors
    public UsageLog() {}
    
    public UsageLog(String tenantId, String userId, String sessionId, String requestId, 
                   Integer knowledgeTokensUsed, Integer llmInputTokens, Integer llmOutputTokens, 
                   BigDecimal costEstimate, String model) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.sessionId = sessionId;
        this.requestId = requestId;
        this.knowledgeTokensUsed = knowledgeTokensUsed;
        this.llmInputTokens = llmInputTokens;
        this.llmOutputTokens = llmOutputTokens;
        this.costEstimate = costEstimate;
        this.model = model;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public Integer getKnowledgeTokensUsed() {
        return knowledgeTokensUsed;
    }
    
    public void setKnowledgeTokensUsed(Integer knowledgeTokensUsed) {
        this.knowledgeTokensUsed = knowledgeTokensUsed;
    }
    
    public Integer getLlmInputTokens() {
        return llmInputTokens;
    }
    
    public void setLlmInputTokens(Integer llmInputTokens) {
        this.llmInputTokens = llmInputTokens;
    }
    
    public Integer getLlmOutputTokens() {
        return llmOutputTokens;
    }
    
    public void setLlmOutputTokens(Integer llmOutputTokens) {
        this.llmOutputTokens = llmOutputTokens;
    }
    
    public BigDecimal getCostEstimate() {
        return costEstimate;
    }
    
    public void setCostEstimate(BigDecimal costEstimate) {
        this.costEstimate = costEstimate;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

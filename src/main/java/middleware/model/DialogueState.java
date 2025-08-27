package middleware.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dialogue_states")
public class DialogueState {
    
    @Id
    private String id;
    
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    
    @Column(name = "session_id", nullable = false)
    private String sessionId;
    
    @Column(name = "user_id")
    private String userId;
    
    @Column(name = "summary_short", columnDefinition = "TEXT")
    private String summaryShort; // ≤250 tokens
    
    @Column(name = "summary_bullets", columnDefinition = "TEXT")
    private String summaryBullets; // ≤120 tokens
    
    @Column(name = "topics", columnDefinition = "TEXT")
    private String topics; // JSON array of topics
    
    @Column(name = "last_updated_at", nullable = false)
    private LocalDateTime lastUpdatedAt;
    
    @Column(name = "cumulative_tokens")
    private Integer cumulativeTokens;
    
    @Column(name = "turn_count")
    private Integer turnCount;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON object
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public DialogueState() {}
    
    public DialogueState(String tenantId, String sessionId, String userId) {
        this.tenantId = tenantId;
        this.sessionId = sessionId;
        this.userId = userId;
        this.turnCount = 0;
        this.cumulativeTokens = 0;
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
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getSummaryShort() {
        return summaryShort;
    }
    
    public void setSummaryShort(String summaryShort) {
        this.summaryShort = summaryShort;
    }
    
    public String getSummaryBullets() {
        return summaryBullets;
    }
    
    public void setSummaryBullets(String summaryBullets) {
        this.summaryBullets = summaryBullets;
    }
    
    public String getTopics() {
        return topics;
    }
    
    public void setTopics(String topics) {
        this.topics = topics;
    }
    
    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }
    
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
    
    public Integer getCumulativeTokens() {
        return cumulativeTokens;
    }
    
    public void setCumulativeTokens(Integer cumulativeTokens) {
        this.cumulativeTokens = cumulativeTokens;
    }
    
    public Integer getTurnCount() {
        return turnCount;
    }
    
    public void setTurnCount(Integer turnCount) {
        this.turnCount = turnCount;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}

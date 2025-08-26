package middleware.service;

import java.util.List;
import java.util.Map;

/**
 * Service interface for managing dialogue state and conversation context.
 * Handles session management, summarization, and state persistence.
 */
public interface DialogueStateService {
    
    /**
     * Get or create dialogue state for a session.
     * 
     * @param tenantId The tenant ID
     * @param sessionId The session ID
     * @param userId The user ID
     * @return The dialogue state
     */
    DialogueState getOrCreateDialogueState(String tenantId, String sessionId, String userId);
    
    /**
     * Update dialogue state with new conversation turn.
     * 
     * @param sessionId The session ID
     * @param userMessage The user message
     * @param assistantMessage The assistant response
     * @param knowledgeTokens Number of knowledge tokens used
     */
    void updateDialogueState(String sessionId, String userMessage, String assistantMessage, int knowledgeTokens);
    
    /**
     * Summarize the dialogue state.
     * 
     * @param sessionId The session ID
     * @return The updated dialogue state with new summary
     */
    DialogueState summarizeDialogueState(String sessionId);
    
    /**
     * Get recent dialogue states for a user.
     * 
     * @param tenantId The tenant ID
     * @param userId The user ID
     * @param limit Maximum number of states to return
     * @return List of recent dialogue states
     */
    List<DialogueState> getRecentDialogueStates(String tenantId, String userId, int limit);
    
    /**
     * Clean up old dialogue states.
     * 
     * @param tenantId The tenant ID
     * @param olderThanDays Remove states older than this many days
     * @return Number of states removed
     */
    int cleanupOldDialogueStates(String tenantId, int olderThanDays);
    
    /**
     * DTO for dialogue state.
     */
    class DialogueState {
        private String id;
        private String tenantId;
        private String sessionId;
        private String userId;
        private String summaryShort;
        private String summaryBullets;
        private List<String> topics;
        private int cumulativeTokens;
        private java.time.LocalDateTime lastUpdatedAt;
        private Map<String, Object> metadata;
        
        public DialogueState(String id, String tenantId, String sessionId, String userId) {
            this.id = id;
            this.tenantId = tenantId;
            this.sessionId = sessionId;
            this.userId = userId;
            this.lastUpdatedAt = java.time.LocalDateTime.now();
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
        
        public List<String> getTopics() {
            return topics;
        }
        
        public void setTopics(List<String> topics) {
            this.topics = topics;
        }
        
        public int getCumulativeTokens() {
            return cumulativeTokens;
        }
        
        public void setCumulativeTokens(int cumulativeTokens) {
            this.cumulativeTokens = cumulativeTokens;
        }
        
        public java.time.LocalDateTime getLastUpdatedAt() {
            return lastUpdatedAt;
        }
        
        public void setLastUpdatedAt(java.time.LocalDateTime lastUpdatedAt) {
            this.lastUpdatedAt = lastUpdatedAt;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        
        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }
}

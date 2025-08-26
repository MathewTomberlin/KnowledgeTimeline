package middleware.service;

import java.time.LocalDateTime;

/**
 * Service interface for tracking usage and costs.
 * Provides comprehensive tracking of token usage, costs, and API calls.
 */
public interface UsageTrackingService {
    
    /**
     * Track a chat completion request.
     * 
     * @param tenantId The tenant ID
     * @param userId The user ID
     * @param sessionId The session ID
     * @param requestId The request ID
     * @param model The model used
     * @param promptTokens Number of prompt tokens
     * @param completionTokens Number of completion tokens
     * @param knowledgeTokens Number of knowledge tokens used
     * @param costEstimate Estimated cost
     */
    void trackChatCompletion(String tenantId, String userId, String sessionId, String requestId,
                           String model, int promptTokens, int completionTokens, int knowledgeTokens,
                           double costEstimate);
    
    /**
     * Track an embedding request.
     * 
     * @param tenantId The tenant ID
     * @param userId The user ID
     * @param sessionId The session ID
     * @param requestId The request ID
     * @param model The model used
     * @param tokens Number of tokens
     * @param costEstimate Estimated cost
     */
    void trackEmbedding(String tenantId, String userId, String sessionId, String requestId,
                       String model, int tokens, double costEstimate);
    
    /**
     * Get usage statistics for a tenant.
     * 
     * @param tenantId The tenant ID
     * @param startDate Start date for the period
     * @param endDate End date for the period
     * @return Usage statistics
     */
    UsageStatistics getUsageStatistics(String tenantId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Check if a tenant has exceeded their rate limits.
     * 
     * @param tenantId The tenant ID
     * @return true if rate limit exceeded
     */
    boolean isRateLimitExceeded(String tenantId);
    
    /**
     * Get the current usage for a tenant.
     * 
     * @param tenantId The tenant ID
     * @return Current usage information
     */
    CurrentUsage getCurrentUsage(String tenantId);
    
    /**
     * DTO for usage statistics.
     */
    class UsageStatistics {
        private int totalRequests;
        private int totalTokens;
        private double totalCost;
        private double averageResponseTime;
        private java.util.Map<String, ModelUsage> byModel;
        
        public UsageStatistics(int totalRequests, int totalTokens, double totalCost,
                             double averageResponseTime, java.util.Map<String, ModelUsage> byModel) {
            this.totalRequests = totalRequests;
            this.totalTokens = totalTokens;
            this.totalCost = totalCost;
            this.averageResponseTime = averageResponseTime;
            this.byModel = byModel;
        }
        
        // Getters and Setters
        public int getTotalRequests() {
            return totalRequests;
        }
        
        public void setTotalRequests(int totalRequests) {
            this.totalRequests = totalRequests;
        }
        
        public int getTotalTokens() {
            return totalTokens;
        }
        
        public void setTotalTokens(int totalTokens) {
            this.totalTokens = totalTokens;
        }
        
        public double getTotalCost() {
            return totalCost;
        }
        
        public void setTotalCost(double totalCost) {
            this.totalCost = totalCost;
        }
        
        public double getAverageResponseTime() {
            return averageResponseTime;
        }
        
        public void setAverageResponseTime(double averageResponseTime) {
            this.averageResponseTime = averageResponseTime;
        }
        
        public java.util.Map<String, ModelUsage> getByModel() {
            return byModel;
        }
        
        public void setByModel(java.util.Map<String, ModelUsage> byModel) {
            this.byModel = byModel;
        }
    }
    
    /**
     * DTO for model-specific usage.
     */
    class ModelUsage {
        private int requests;
        private int tokens;
        private double cost;
        
        public ModelUsage(int requests, int tokens, double cost) {
            this.requests = requests;
            this.tokens = tokens;
            this.cost = cost;
        }
        
        // Getters and Setters
        public int getRequests() {
            return requests;
        }
        
        public void setRequests(int requests) {
            this.requests = requests;
        }
        
        public int getTokens() {
            return tokens;
        }
        
        public void setTokens(int tokens) {
            this.tokens = tokens;
        }
        
        public double getCost() {
            return cost;
        }
        
        public void setCost(double cost) {
            this.cost = cost;
        }
    }
    
    /**
     * DTO for current usage information.
     */
    class CurrentUsage {
        private int requestsThisMinute;
        private int tokensThisMinute;
        private double costThisMinute;
        private int requestsThisHour;
        private int tokensThisHour;
        private double costThisHour;
        
        public CurrentUsage(int requestsThisMinute, int tokensThisMinute, double costThisMinute,
                          int requestsThisHour, int tokensThisHour, double costThisHour) {
            this.requestsThisMinute = requestsThisMinute;
            this.tokensThisMinute = tokensThisMinute;
            this.costThisMinute = costThisMinute;
            this.requestsThisHour = requestsThisHour;
            this.tokensThisHour = tokensThisHour;
            this.costThisHour = costThisHour;
        }
        
        // Getters and Setters
        public int getRequestsThisMinute() {
            return requestsThisMinute;
        }
        
        public void setRequestsThisMinute(int requestsThisMinute) {
            this.requestsThisMinute = requestsThisMinute;
        }
        
        public int getTokensThisMinute() {
            return tokensThisMinute;
        }
        
        public void setTokensThisMinute(int tokensThisMinute) {
            this.tokensThisMinute = tokensThisMinute;
        }
        
        public double getCostThisMinute() {
            return costThisMinute;
        }
        
        public void setCostThisMinute(double costThisMinute) {
            this.costThisMinute = costThisMinute;
        }
        
        public int getRequestsThisHour() {
            return requestsThisHour;
        }
        
        public void setRequestsThisHour(int requestsThisHour) {
            this.requestsThisHour = requestsThisHour;
        }
        
        public int getTokensThisHour() {
            return tokensThisHour;
        }
        
        public void setTokensThisHour(int tokensThisHour) {
            this.tokensThisHour = tokensThisHour;
        }
        
        public double getCostThisHour() {
            return costThisHour;
        }
        
        public void setCostThisHour(double costThisHour) {
            this.costThisHour = costThisHour;
        }
    }
}

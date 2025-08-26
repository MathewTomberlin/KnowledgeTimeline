package middleware.service.impl;

import middleware.service.UsageTrackingService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of UsageTrackingService for testing and local development.
 * Provides simulated usage tracking functionality.
 */
@Service
public class MockUsageTrackingService implements UsageTrackingService {

    private final Map<String, Map<String, Long>> tenantUsage = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Double>> tenantCosts = new ConcurrentHashMap<>();

    @Override
    public void trackChatCompletion(String tenantId, String userId, String sessionId, String requestId,
                                   String model, int promptTokens, int completionTokens, int knowledgeTokens,
                                   double costEstimate) {
        // Track tokens
        tenantUsage.computeIfAbsent(tenantId, k -> new ConcurrentHashMap<>())
                  .merge("total_tokens", (long) (promptTokens + completionTokens), Long::sum);
        tenantUsage.computeIfAbsent(tenantId, k -> new ConcurrentHashMap<>())
                  .merge("knowledge_tokens", (long) knowledgeTokens, Long::sum);

        // Track costs
        tenantCosts.computeIfAbsent(tenantId, k -> new ConcurrentHashMap<>())
                  .merge("total_cost", costEstimate, Double::sum);
        tenantCosts.computeIfAbsent(tenantId, k -> new ConcurrentHashMap<>())
                  .merge(model + "_cost", costEstimate, Double::sum);
    }

    @Override
    public void trackEmbedding(String tenantId, String userId, String sessionId, String requestId,
                              String model, int tokens, double costEstimate) {
        // Track embedding tokens
        tenantUsage.computeIfAbsent(tenantId, k -> new ConcurrentHashMap<>())
                  .merge("embedding_tokens", (long) tokens, Long::sum);

        // Track embedding costs
        tenantCosts.computeIfAbsent(tenantId, k -> new ConcurrentHashMap<>())
                  .merge("embedding_cost", costEstimate, Double::sum);
    }

    @Override
    public UsageStatistics getUsageStatistics(String tenantId, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Long> usage = tenantUsage.getOrDefault(tenantId, new HashMap<>());
        Map<String, Double> costs = tenantCosts.getOrDefault(tenantId, new HashMap<>());

        Map<String, ModelUsage> byModel = new HashMap<>();
        byModel.put("gpt-3.5-turbo", new ModelUsage(
            usage.getOrDefault("gpt-3.5-turbo_requests", 0L).intValue(),
            usage.getOrDefault("gpt-3.5-turbo_tokens", 0L).intValue(),
            costs.getOrDefault("gpt-3.5-turbo_cost", 0.0)
        ));
        byModel.put("text-embedding-ada-002", new ModelUsage(
            usage.getOrDefault("embedding_requests", 0L).intValue(),
            usage.getOrDefault("embedding_tokens", 0L).intValue(),
            costs.getOrDefault("embedding_cost", 0.0)
        ));

        return new UsageStatistics(
            usage.getOrDefault("total_requests", 0L).intValue(),
            usage.getOrDefault("total_tokens", 0L).intValue(),
            costs.getOrDefault("total_cost", 0.0),
            0.0, // Mock average response time
            byModel
        );
    }

    @Override
    public boolean isRateLimitExceeded(String tenantId) {
        // Mock rate limiting - allow all requests for testing
        return false;
    }

    @Override
    public CurrentUsage getCurrentUsage(String tenantId) {
        Map<String, Long> usage = tenantUsage.getOrDefault(tenantId, new HashMap<>());
        Map<String, Double> costs = tenantCosts.getOrDefault(tenantId, new HashMap<>());

        return new CurrentUsage(
            usage.getOrDefault("requests_this_minute", 0L).intValue(),
            usage.getOrDefault("tokens_this_minute", 0L).intValue(),
            costs.getOrDefault("cost_this_minute", 0.0),
            usage.getOrDefault("requests_this_hour", 0L).intValue(),
            usage.getOrDefault("tokens_this_hour", 0L).intValue(),
            costs.getOrDefault("cost_this_hour", 0.0)
        );
    }
}

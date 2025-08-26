package middleware.service.impl;

import middleware.model.UsageLog;
import middleware.repository.UsageLogRepository;
import middleware.service.UsageTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Real implementation of UsageTrackingService with database persistence and Redis-based rate limiting.
 * Provides comprehensive usage tracking, cost estimation, and analytics.
 */
@Service
public class RealUsageTrackingService implements UsageTrackingService {

    private static final Logger logger = LoggerFactory.getLogger(RealUsageTrackingService.class);
    
    @Autowired
    private UsageLogRepository usageLogRepository;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    // Rate limiting configuration
    private static final int DEFAULT_RATE_LIMIT_PER_MINUTE = 100;
    private static final int DEFAULT_RATE_LIMIT_PER_HOUR = 1000;
    private static final int DEFAULT_RATE_LIMIT_PER_DAY = 10000;
    
    // Cost estimation (per 1K tokens)
    private static final Map<String, Double> MODEL_COSTS = Map.of(
        "gpt-3.5-turbo", 0.002,      // $0.002 per 1K input tokens
        "gpt-4", 0.03,               // $0.03 per 1K input tokens
        "text-embedding-ada-002", 0.0001  // $0.0001 per 1K tokens
    );

    @Override
    public void trackChatCompletion(String tenantId, String userId, String sessionId, String requestId,
                                   String model, int promptTokens, int completionTokens, int knowledgeTokens,
                                   double costEstimate) {
        logger.debug("Tracking chat completion for tenant: {}, model: {}, tokens: {}+{}", 
                    tenantId, model, promptTokens, completionTokens);
        
        try {
            // Create usage log entry
            UsageLog usageLog = new UsageLog();
            usageLog.setTenantId(tenantId);
            usageLog.setUserId(userId);
            usageLog.setSessionId(sessionId);
            usageLog.setRequestId(requestId);
            usageLog.setModel(model);
            usageLog.setLlmInputTokens(promptTokens);
            usageLog.setLlmOutputTokens(completionTokens);
            usageLog.setKnowledgeTokensUsed(knowledgeTokens);
            usageLog.setCostEstimate(BigDecimal.valueOf(costEstimate));
            usageLog.setTimestamp(LocalDateTime.now());
            
            // Save to database
            usageLogRepository.save(usageLog);
            
            // Update Redis counters for rate limiting
            updateRedisCounters(tenantId, promptTokens + completionTokens, costEstimate);
            
            logger.debug("Successfully tracked chat completion for tenant: {}", tenantId);
            
        } catch (Exception e) {
            logger.error("Error tracking chat completion for tenant: {}", tenantId, e);
        }
    }

    @Override
    public void trackEmbedding(String tenantId, String userId, String sessionId, String requestId,
                              String model, int tokens, double costEstimate) {
        logger.debug("Tracking embedding for tenant: {}, model: {}, tokens: {}", 
                    tenantId, model, tokens);
        
        try {
            // Create usage log entry
            UsageLog usageLog = new UsageLog();
            usageLog.setTenantId(tenantId);
            usageLog.setUserId(userId);
            usageLog.setSessionId(sessionId);
            usageLog.setRequestId(requestId);
            usageLog.setModel(model);
            usageLog.setLlmInputTokens(tokens);
            usageLog.setLlmOutputTokens(0);
            usageLog.setKnowledgeTokensUsed(0);
            usageLog.setCostEstimate(BigDecimal.valueOf(costEstimate));
            usageLog.setTimestamp(LocalDateTime.now());
            
            // Save to database
            usageLogRepository.save(usageLog);
            
            // Update Redis counters for rate limiting
            updateRedisCounters(tenantId, tokens, costEstimate);
            
            logger.debug("Successfully tracked embedding for tenant: {}", tenantId);
            
        } catch (Exception e) {
            logger.error("Error tracking embedding for tenant: {}", tenantId, e);
        }
    }

    @Override
    public UsageStatistics getUsageStatistics(String tenantId, LocalDateTime startDate, LocalDateTime endDate) {
        logger.debug("Getting usage statistics for tenant: {} from {} to {}", 
                    tenantId, startDate, endDate);
        
        try {
            // Query usage logs from database
            List<UsageLog> usageLogs = usageLogRepository.findByTenantIdAndTimestampBetween(
                tenantId, startDate, endDate, org.springframework.data.domain.Pageable.unpaged()).getContent();
            
            // Calculate statistics
            int totalRequests = usageLogs.size();
            int totalTokens = usageLogs.stream()
                .mapToInt(log -> log.getLlmInputTokens() + log.getLlmOutputTokens())
                .sum();
            double totalCost = usageLogs.stream()
                .mapToDouble(log -> log.getCostEstimate().doubleValue())
                .sum();
            
            // Calculate average response time (mock for now)
            double averageResponseTime = 0.5; // Mock value
            
            // Group by model
            Map<String, ModelUsage> byModel = usageLogs.stream()
                .collect(Collectors.groupingBy(
                    UsageLog::getModel,
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        logs -> {
                            int requests = logs.size();
                            int tokens = logs.stream()
                                .mapToInt(log -> log.getLlmInputTokens() + log.getLlmOutputTokens())
                                .sum();
                            double cost = logs.stream()
                                .mapToDouble(log -> log.getCostEstimate().doubleValue())
                                .sum();
                            return new ModelUsage(requests, tokens, cost);
                        }
                    )
                ));
            
            logger.debug("Retrieved usage statistics for tenant: {} - {} requests, {} tokens, ${:.4f} cost", 
                        tenantId, totalRequests, totalTokens, totalCost);
            
            return new UsageStatistics(totalRequests, totalTokens, totalCost, averageResponseTime, byModel);
            
        } catch (Exception e) {
            logger.error("Error getting usage statistics for tenant: {}", tenantId, e);
            return new UsageStatistics(0, 0, 0.0, 0.0, new HashMap<>());
        }
    }

    @Override
    public boolean isRateLimitExceeded(String tenantId) {
        try {
            // Check minute rate limit
            String minuteKey = "rate_limit:" + tenantId + ":minute:" + getCurrentMinute();
            String minuteCount = redisTemplate.opsForValue().get(minuteKey);
            int minuteRequests = minuteCount != null ? Integer.parseInt(minuteCount) : 0;
            
            if (minuteRequests >= DEFAULT_RATE_LIMIT_PER_MINUTE) {
                logger.warn("Rate limit exceeded for tenant: {} - {} requests per minute", 
                           tenantId, minuteRequests);
                return true;
            }
            
            // Check hour rate limit
            String hourKey = "rate_limit:" + tenantId + ":hour:" + getCurrentHour();
            String hourCount = redisTemplate.opsForValue().get(hourKey);
            int hourRequests = hourCount != null ? Integer.parseInt(hourCount) : 0;
            
            if (hourRequests >= DEFAULT_RATE_LIMIT_PER_HOUR) {
                logger.warn("Rate limit exceeded for tenant: {} - {} requests per hour", 
                           tenantId, hourRequests);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            logger.error("Error checking rate limit for tenant: {}", tenantId, e);
            return false; // Allow requests if rate limiting fails
        }
    }

    @Override
    public CurrentUsage getCurrentUsage(String tenantId) {
        try {
            // Get current minute usage
            String minuteKey = "rate_limit:" + tenantId + ":minute:" + getCurrentMinute();
            String minuteCount = redisTemplate.opsForValue().get(minuteKey);
            int requestsThisMinute = minuteCount != null ? Integer.parseInt(minuteCount) : 0;
            
            // Get current hour usage
            String hourKey = "rate_limit:" + tenantId + ":hour:" + getCurrentHour();
            String hourCount = redisTemplate.opsForValue().get(hourKey);
            int requestsThisHour = hourCount != null ? Integer.parseInt(hourCount) : 0;
            
            // Get token and cost usage from Redis
            String minuteTokensKey = "tokens:" + tenantId + ":minute:" + getCurrentMinute();
            String minuteTokens = redisTemplate.opsForValue().get(minuteTokensKey);
            int tokensThisMinute = minuteTokens != null ? Integer.parseInt(minuteTokens) : 0;
            
            String hourTokensKey = "tokens:" + tenantId + ":hour:" + getCurrentHour();
            String hourTokens = redisTemplate.opsForValue().get(hourTokensKey);
            int tokensThisHour = hourTokens != null ? Integer.parseInt(hourTokens) : 0;
            
            String minuteCostKey = "cost:" + tenantId + ":minute:" + getCurrentMinute();
            String minuteCost = redisTemplate.opsForValue().get(minuteCostKey);
            double costThisMinute = minuteCost != null ? Double.parseDouble(minuteCost) : 0.0;
            
            String hourCostKey = "cost:" + tenantId + ":hour:" + getCurrentHour();
            String hourCost = redisTemplate.opsForValue().get(hourCostKey);
            double costThisHour = hourCost != null ? Double.parseDouble(hourCost) : 0.0;
            
            return new CurrentUsage(
                requestsThisMinute, tokensThisMinute, costThisMinute,
                requestsThisHour, tokensThisHour, costThisHour
            );
            
        } catch (Exception e) {
            logger.error("Error getting current usage for tenant: {}", tenantId, e);
            return new CurrentUsage(0, 0, 0.0, 0, 0, 0.0);
        }
    }
    
    /**
     * Update Redis counters for rate limiting and usage tracking.
     */
    private void updateRedisCounters(String tenantId, int tokens, double cost) {
        try {
            String currentMinute = getCurrentMinute();
            String currentHour = getCurrentHour();
            
            // Update request counts
            String minuteKey = "rate_limit:" + tenantId + ":minute:" + currentMinute;
            redisTemplate.opsForValue().increment(minuteKey);
            redisTemplate.expire(minuteKey, 60, TimeUnit.SECONDS);
            
            String hourKey = "rate_limit:" + tenantId + ":hour:" + currentHour;
            redisTemplate.opsForValue().increment(hourKey);
            redisTemplate.expire(hourKey, 3600, TimeUnit.SECONDS);
            
            // Update token counts
            String minuteTokensKey = "tokens:" + tenantId + ":minute:" + currentMinute;
            redisTemplate.opsForValue().increment(minuteTokensKey, tokens);
            redisTemplate.expire(minuteTokensKey, 60, TimeUnit.SECONDS);
            
            String hourTokensKey = "tokens:" + tenantId + ":hour:" + currentHour;
            redisTemplate.opsForValue().increment(hourTokensKey, tokens);
            redisTemplate.expire(hourTokensKey, 3600, TimeUnit.SECONDS);
            
            // Update cost counts
            String minuteCostKey = "cost:" + tenantId + ":minute:" + currentMinute;
            redisTemplate.opsForValue().increment(minuteCostKey, cost);
            redisTemplate.expire(minuteCostKey, 60, TimeUnit.SECONDS);
            
            String hourCostKey = "cost:" + tenantId + ":hour:" + currentHour;
            redisTemplate.opsForValue().increment(hourCostKey, cost);
            redisTemplate.expire(hourCostKey, 3600, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            logger.error("Error updating Redis counters for tenant: {}", tenantId, e);
        }
    }
    
    /**
     * Get current minute as string for Redis key.
     */
    private String getCurrentMinute() {
        LocalDateTime now = LocalDateTime.now();
        return String.format("%04d%02d%02d%02d%02d", 
            now.getYear(), now.getMonthValue(), now.getDayOfMonth(), 
            now.getHour(), now.getMinute());
    }
    
    /**
     * Get current hour as string for Redis key.
     */
    private String getCurrentHour() {
        LocalDateTime now = LocalDateTime.now();
        return String.format("%04d%02d%02d%02d", 
            now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now.getHour());
    }
    
    /**
     * Estimate cost for a model based on token usage.
     */
    public double estimateCost(String model, int inputTokens, int outputTokens) {
        double inputCost = MODEL_COSTS.getOrDefault(model, 0.002) * inputTokens / 1000.0;
        double outputCost = MODEL_COSTS.getOrDefault(model, 0.002) * outputTokens / 1000.0;
        return inputCost + outputCost;
    }
}

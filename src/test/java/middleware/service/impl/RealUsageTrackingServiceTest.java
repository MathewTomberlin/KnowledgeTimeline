package middleware.service.impl;

import middleware.model.UsageLog;
import middleware.repository.UsageLogRepository;
import middleware.service.UsageTrackingService;
import middleware.service.UsageTrackingService.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RealUsageTrackingServiceTest {

    @Mock
    private UsageLogRepository usageLogRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RealUsageTrackingService usageTrackingService;

    private UsageLog mockUsageLog;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @BeforeEach
    void setUp() {
        mockUsageLog = new UsageLog();
        mockUsageLog.setId("log1");
        mockUsageLog.setTenantId("tenant1");
        mockUsageLog.setUserId("user1");
        mockUsageLog.setSessionId("session1");
        mockUsageLog.setRequestId("req1");
        mockUsageLog.setModel("gpt-3.5-turbo");
        mockUsageLog.setLlmInputTokens(100);
        mockUsageLog.setLlmOutputTokens(50);
        mockUsageLog.setKnowledgeTokensUsed(25);
        mockUsageLog.setCostEstimate(BigDecimal.valueOf(0.002));
        mockUsageLog.setTimestamp(LocalDateTime.now());
        
        startDate = LocalDateTime.now().minusDays(7);
        endDate = LocalDateTime.now();
    }

    @Test
    void testTrackChatCompletion_WithValidInput() {
        // Arrange
        String tenantId = "tenant1";
        String userId = "user1";
        String sessionId = "session1";
        String requestId = "req1";
        String model = "gpt-3.5-turbo";
        int promptTokens = 100;
        int completionTokens = 50;
        int knowledgeTokens = 25;
        double costEstimate = 0.002;

        when(usageLogRepository.save(any(UsageLog.class))).thenReturn(mockUsageLog);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        usageTrackingService.trackChatCompletion(tenantId, userId, sessionId, requestId, 
                                                model, promptTokens, completionTokens, knowledgeTokens, costEstimate);

        // Assert
        verify(usageLogRepository).save(argThat(log -> 
            log.getTenantId().equals(tenantId) &&
            log.getUserId().equals(userId) &&
            log.getSessionId().equals(sessionId) &&
            log.getRequestId().equals(requestId) &&
            log.getModel().equals(model) &&
            log.getLlmInputTokens() == promptTokens &&
            log.getLlmOutputTokens() == completionTokens &&
            log.getKnowledgeTokensUsed() == knowledgeTokens &&
            log.getCostEstimate().doubleValue() == costEstimate
        ));
        
        // Verify Redis operations - the service calls increment 2 times and expire 6 times
        verify(valueOperations, times(2)).increment(anyString());
        verify(redisTemplate, times(6)).expire(anyString(), anyLong(), any());
    }

    @Test
    void testTrackChatCompletion_WhenRepositoryThrowsException() {
        // Arrange
        when(usageLogRepository.save(any(UsageLog.class)))
            .thenThrow(new RuntimeException("Database error"));

        // Act & Assert - Should not throw exception, should handle gracefully
        assertDoesNotThrow(() -> 
            usageTrackingService.trackChatCompletion("tenant1", "user1", "session1", "req1",
                                                   "gpt-3.5-turbo", 100, 50, 25, 0.002)
        );
    }

    @Test
    void testTrackEmbedding_WithValidInput() {
        // Arrange
        String tenantId = "tenant1";
        String userId = "user1";
        String sessionId = "session1";
        String requestId = "req1";
        String model = "text-embedding-ada-002";
        int tokens = 100;
        double costEstimate = 0.0001;

        when(usageLogRepository.save(any(UsageLog.class))).thenReturn(mockUsageLog);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        usageTrackingService.trackEmbedding(tenantId, userId, sessionId, requestId, model, tokens, costEstimate);

        // Assert
        verify(usageLogRepository).save(argThat(log -> 
            log.getTenantId().equals(tenantId) &&
            log.getUserId().equals(userId) &&
            log.getSessionId().equals(sessionId) &&
            log.getRequestId().equals(requestId) &&
            log.getModel().equals(model) &&
            log.getLlmInputTokens() == tokens &&
            log.getLlmOutputTokens() == 0 &&
            log.getKnowledgeTokensUsed() == 0 &&
            log.getCostEstimate().doubleValue() == costEstimate
        ));
        
        // Verify Redis operations - the service calls increment 2 times and expire 6 times
        verify(valueOperations, times(2)).increment(anyString());
        verify(redisTemplate, times(6)).expire(anyString(), anyLong(), any());
    }

    @Test
    void testGetUsageStatistics_WithValidData() {
        // Arrange
        List<UsageLog> usageLogs = Arrays.asList(
            createUsageLog("gpt-3.5-turbo", 100, 50, 0.002),
            createUsageLog("gpt-4", 200, 100, 0.006),
            createUsageLog("gpt-3.5-turbo", 150, 75, 0.003)
        );
        
        Page<UsageLog> page = new PageImpl<>(usageLogs);
        when(usageLogRepository.findByTenantIdAndTimestampBetween(
            eq("tenant1"), eq(startDate), eq(endDate), any(Pageable.class)))
            .thenReturn(page);

        // Act
        UsageStatistics result = usageTrackingService.getUsageStatistics("tenant1", startDate, endDate);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getTotalRequests());
        assertEquals(675, result.getTotalTokens()); // (100+50) + (200+100) + (150+75)
        assertEquals(0.011, result.getTotalCost(), 0.001); // 0.002 + 0.006 + 0.003
        assertEquals(0.5, result.getAverageResponseTime(), 0.01);
        
        // Check model breakdown
        Map<String, ModelUsage> byModel = result.getByModel();
        assertEquals(2, byModel.size());
        
        ModelUsage gpt35Usage = byModel.get("gpt-3.5-turbo");
        assertNotNull(gpt35Usage);
        assertEquals(2, gpt35Usage.getRequests());
        assertEquals(375, gpt35Usage.getTokens()); // (100+50) + (150+75)
        assertEquals(0.005, gpt35Usage.getCost(), 0.001); // 0.002 + 0.003
        
        ModelUsage gpt4Usage = byModel.get("gpt-4");
        assertNotNull(gpt4Usage);
        assertEquals(1, gpt4Usage.getRequests());
        assertEquals(300, gpt4Usage.getTokens()); // 200+100
        assertEquals(0.006, gpt4Usage.getCost(), 0.001);
    }

    @Test
    void testGetUsageStatistics_WithEmptyData() {
        // Arrange
        Page<UsageLog> emptyPage = new PageImpl<>(new ArrayList<>());
        when(usageLogRepository.findByTenantIdAndTimestampBetween(
            eq("tenant1"), eq(startDate), eq(endDate), any(Pageable.class)))
            .thenReturn(emptyPage);

        // Act
        UsageStatistics result = usageTrackingService.getUsageStatistics("tenant1", startDate, endDate);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalRequests());
        assertEquals(0, result.getTotalTokens());
        assertEquals(0.0, result.getTotalCost(), 0.001);
        assertEquals(0.5, result.getAverageResponseTime(), 0.01);
        assertTrue(result.getByModel().isEmpty());
    }

    @Test
    void testGetUsageStatistics_WhenRepositoryThrowsException() {
        // Arrange
        when(usageLogRepository.findByTenantIdAndTimestampBetween(
            eq("tenant1"), eq(startDate), eq(endDate), any(Pageable.class)))
            .thenThrow(new RuntimeException("Database error"));

        // Act
        UsageStatistics result = usageTrackingService.getUsageStatistics("tenant1", startDate, endDate);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalRequests());
        assertEquals(0, result.getTotalTokens());
        assertEquals(0.0, result.getTotalCost(), 0.001);
        assertEquals(0.0, result.getAverageResponseTime(), 0.01);
        assertTrue(result.getByModel().isEmpty());
    }

    @Test
    void testIsRateLimitExceeded_WhenNotExceeded() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(contains("minute"))).thenReturn("50"); // Below limit of 100
        when(valueOperations.get(contains("hour"))).thenReturn("500"); // Below limit of 1000

        // Act
        boolean result = usageTrackingService.isRateLimitExceeded("tenant1");

        // Assert
        assertFalse(result);
        verify(valueOperations, times(2)).get(anyString());
    }

    @Test
    void testIsRateLimitExceeded_WhenMinuteLimitExceeded() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(contains("minute"))).thenReturn("150"); // Above limit of 100

        // Act
        boolean result = usageTrackingService.isRateLimitExceeded("tenant1");

        // Assert
        assertTrue(result);
        verify(valueOperations, times(1)).get(anyString()); // Should stop after minute check
    }

    @Test
    void testIsRateLimitExceeded_WhenHourLimitExceeded() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(contains("minute"))).thenReturn("50"); // Below minute limit
        when(valueOperations.get(contains("hour"))).thenReturn("1500"); // Above hour limit of 1000

        // Act
        boolean result = usageTrackingService.isRateLimitExceeded("tenant1");

        // Assert
        assertTrue(result);
        verify(valueOperations, times(2)).get(anyString());
    }

    @Test
    void testIsRateLimitExceeded_WhenRedisThrowsException() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis error"));

        // Act
        boolean result = usageTrackingService.isRateLimitExceeded("tenant1");

        // Assert
        assertFalse(result); // Should allow requests when rate limiting fails
    }

    @Test
    void testGetCurrentUsage_WithValidData() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("25"); // Default for all keys

        // Act
        CurrentUsage result = usageTrackingService.getCurrentUsage("tenant1");

        // Assert
        assertNotNull(result);
        assertEquals(25, result.getRequestsThisMinute());
        assertEquals(25, result.getTokensThisMinute());
        assertEquals(25.0, result.getCostThisMinute(), 0.001);
        assertEquals(25, result.getRequestsThisHour());
        assertEquals(25, result.getTokensThisHour());
        assertEquals(25.0, result.getCostThisHour(), 0.001);
    }

    @Test
    void testGetCurrentUsage_WithNullValues() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        // Act
        CurrentUsage result = usageTrackingService.getCurrentUsage("tenant1");

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getRequestsThisMinute());
        assertEquals(0, result.getTokensThisMinute());
        assertEquals(0.0, result.getCostThisMinute(), 0.001);
        assertEquals(0, result.getRequestsThisHour());
        assertEquals(0, result.getTokensThisHour());
        assertEquals(0.0, result.getCostThisHour(), 0.001);
    }

    @Test
    void testGetCurrentUsage_WhenRedisThrowsException() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis error"));

        // Act
        CurrentUsage result = usageTrackingService.getCurrentUsage("tenant1");

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getRequestsThisMinute());
        assertEquals(0, result.getTokensThisMinute());
        assertEquals(0.0, result.getCostThisMinute(), 0.001);
        assertEquals(0, result.getRequestsThisHour());
        assertEquals(0, result.getTokensThisHour());
        assertEquals(0.0, result.getCostThisHour(), 0.001);
    }

    @Test
    void testEstimateCost_WithKnownModel() {
        // Act
        double result = usageTrackingService.estimateCost("gpt-3.5-turbo", 1000, 500);

        // Assert
        // gpt-3.5-turbo costs $0.002 per 1K tokens
        // Input: 1000 tokens = $0.002, Output: 500 tokens = $0.001
        // Total: $0.003
        assertEquals(0.003, result, 0.001);
    }

    @Test
    void testEstimateCost_WithGpt4Model() {
        // Act
        double result = usageTrackingService.estimateCost("gpt-4", 1000, 500);

        // Assert
        // gpt-4 costs $0.03 per 1K tokens
        // Input: 1000 tokens = $0.03, Output: 500 tokens = $0.015
        // Total: $0.045
        assertEquals(0.045, result, 0.001);
    }

    @Test
    void testEstimateCost_WithUnknownModel() {
        // Act
        double result = usageTrackingService.estimateCost("unknown-model", 1000, 500);

        // Assert
        // Should use default cost of $0.002 per 1K tokens
        // Input: 1000 tokens = $0.002, Output: 500 tokens = $0.001
        // Total: $0.003
        assertEquals(0.003, result, 0.001);
    }

    @Test
    void testEstimateCost_WithZeroTokens() {
        // Act
        double result = usageTrackingService.estimateCost("gpt-3.5-turbo", 0, 0);

        // Assert
        assertEquals(0.0, result, 0.001);
    }

    private UsageLog createUsageLog(String model, int inputTokens, int outputTokens, double cost) {
        UsageLog log = new UsageLog();
        log.setId(UUID.randomUUID().toString());
        log.setTenantId("tenant1");
        log.setUserId("user1");
        log.setSessionId("session1");
        log.setRequestId("req1");
        log.setModel(model);
        log.setLlmInputTokens(inputTokens);
        log.setLlmOutputTokens(outputTokens);
        log.setKnowledgeTokensUsed(25);
        log.setCostEstimate(BigDecimal.valueOf(cost));
        log.setTimestamp(LocalDateTime.now());
        return log;
    }
}

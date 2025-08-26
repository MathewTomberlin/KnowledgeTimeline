package middleware.repository;

import middleware.model.UsageLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for UsageLog entity operations.
 * Provides data access methods for usage tracking and billing.
 */
@Repository
public interface UsageLogRepository extends JpaRepository<UsageLog, String> {

    /**
     * Find usage logs by tenant.
     *
     * @param tenantId The tenant identifier
     * @param pageable Pagination parameters
     * @return Page of usage logs
     */
    Page<UsageLog> findByTenantId(String tenantId, Pageable pageable);

    /**
     * Find usage logs by user and tenant.
     *
     * @param userId The user identifier
     * @param tenantId The tenant identifier
     * @param pageable Pagination parameters
     * @return Page of usage logs
     */
    Page<UsageLog> findByUserIdAndTenantId(String userId, String tenantId, Pageable pageable);

    /**
     * Find usage logs by session and tenant.
     *
     * @param sessionId The session identifier
     * @param tenantId The tenant identifier
     * @return List of usage logs
     */
    List<UsageLog> findBySessionIdAndTenantId(String sessionId, String tenantId);

    /**
     * Find usage logs by model and tenant.
     *
     * @param model The model name
     * @param tenantId The tenant identifier
     * @param pageable Pagination parameters
     * @return Page of usage logs
     */
    Page<UsageLog> findByModelAndTenantId(String model, String tenantId, Pageable pageable);

    /**
     * Find usage logs within a date range for a tenant.
     *
     * @param tenantId The tenant identifier
     * @param startDate The start date
     * @param endDate The end date
     * @param pageable Pagination parameters
     * @return Page of usage logs
     */
    Page<UsageLog> findByTenantIdAndTimestampBetween(String tenantId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Find usage logs by request ID and tenant.
     *
     * @param requestId The request identifier
     * @param tenantId The tenant identifier
     * @return Optional containing the usage log if found
     */
    @Query("SELECT ul FROM UsageLog ul WHERE ul.requestId = :requestId AND ul.tenantId = :tenantId")
    java.util.Optional<UsageLog> findByRequestIdAndTenantId(@Param("requestId") String requestId, @Param("tenantId") String tenantId);

    /**
     * Calculate total cost for a tenant within a date range.
     *
     * @param tenantId The tenant identifier
     * @param startDate The start date
     * @param endDate The end date
     * @return Total cost
     */
    @Query("SELECT SUM(ul.costEstimate) FROM UsageLog ul WHERE ul.tenantId = :tenantId AND ul.timestamp BETWEEN :startDate AND :endDate")
    Double calculateTotalCostByTenantAndDateRange(@Param("tenantId") String tenantId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate total tokens used for a tenant within a date range.
     *
     * @param tenantId The tenant identifier
     * @param startDate The start date
     * @param endDate The end date
     * @return Total tokens used
     */
    @Query("SELECT SUM(ul.llmInputTokens + ul.llmOutputTokens) FROM UsageLog ul WHERE ul.tenantId = :tenantId AND ul.timestamp BETWEEN :startDate AND :endDate")
    Long calculateTotalTokensByTenantAndDateRange(@Param("tenantId") String tenantId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Calculate total knowledge tokens used for a tenant within a date range.
     *
     * @param tenantId The tenant identifier
     * @param startDate The start date
     * @param endDate The end date
     * @return Total knowledge tokens used
     */
    @Query("SELECT SUM(ul.knowledgeTokensUsed) FROM UsageLog ul WHERE ul.tenantId = :tenantId AND ul.timestamp BETWEEN :startDate AND :endDate")
    Long calculateTotalKnowledgeTokensByTenantAndDateRange(@Param("tenantId") String tenantId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Get usage statistics by model for a tenant within a date range.
     *
     * @param tenantId The tenant identifier
     * @param startDate The start date
     * @param endDate The end date
     * @return List of usage statistics by model
     */
    @Query("SELECT ul.model, COUNT(ul), SUM(ul.llmInputTokens), SUM(ul.llmOutputTokens), SUM(ul.knowledgeTokensUsed), SUM(ul.costEstimate) " +
           "FROM UsageLog ul WHERE ul.tenantId = :tenantId AND ul.timestamp BETWEEN :startDate AND :endDate GROUP BY ul.model")
    List<Object[]> getUsageStatisticsByModel(@Param("tenantId") String tenantId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Count usage logs for a tenant within a date range.
     *
     * @param tenantId The tenant identifier
     * @param startDate The start date
     * @param endDate The end date
     * @return Count of usage logs
     */
    long countByTenantIdAndTimestampBetween(String tenantId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Delete usage logs older than a specified date for a tenant.
     *
     * @param tenantId The tenant identifier
     * @param cutoffDate The cutoff date
     * @return Number of deleted usage logs
     */
    @Query("DELETE FROM UsageLog ul WHERE ul.tenantId = :tenantId AND ul.timestamp < :cutoffDate")
    int deleteOldByTenantId(@Param("tenantId") String tenantId, @Param("cutoffDate") LocalDateTime cutoffDate);
}

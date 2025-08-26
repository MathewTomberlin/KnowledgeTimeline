package middleware.repository;

import middleware.model.DialogueState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for DialogueState entity operations.
 * Provides data access methods for dialogue state management.
 */
@Repository
public interface DialogueStateRepository extends JpaRepository<DialogueState, String> {

    /**
     * Find a dialogue state by its ID and tenant.
     *
     * @param id The dialogue state ID
     * @param tenantId The tenant identifier
     * @return Optional containing the dialogue state if found
     */
    Optional<DialogueState> findByIdAndTenantId(String id, String tenantId);

    /**
     * Find a dialogue state by session ID and tenant.
     *
     * @param sessionId The session identifier
     * @param tenantId The tenant identifier
     * @return Optional containing the dialogue state if found
     */
    Optional<DialogueState> findBySessionIdAndTenantId(String sessionId, String tenantId);

    /**
     * Find all dialogue states for a user and tenant.
     *
     * @param userId The user identifier
     * @param tenantId The tenant identifier
     * @param pageable Pagination parameters
     * @return Page of dialogue states
     */
    Page<DialogueState> findByUserIdAndTenantId(String userId, String tenantId, Pageable pageable);

    /**
     * Find recent dialogue states for a user and tenant.
     *
     * @param userId The user identifier
     * @param tenantId The tenant identifier
     * @param limit Maximum number of results
     * @return List of recent dialogue states
     */
    @Query("SELECT ds FROM DialogueState ds WHERE ds.userId = :userId AND ds.tenantId = :tenantId ORDER BY ds.lastUpdatedAt DESC")
    List<DialogueState> findRecentByUserIdAndTenantId(@Param("userId") String userId, @Param("tenantId") String tenantId, Pageable pageable);

    /**
     * Find dialogue states updated within a date range for a tenant.
     *
     * @param tenantId The tenant identifier
     * @param startDate The start date
     * @param endDate The end date
     * @param pageable Pagination parameters
     * @return Page of dialogue states
     */
    Page<DialogueState> findByTenantIdAndLastUpdatedAtBetween(String tenantId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Find dialogue states by multiple session IDs and tenant.
     *
     * @param sessionIds List of session identifiers
     * @param tenantId The tenant identifier
     * @return List of dialogue states
     */
    @Query("SELECT ds FROM DialogueState ds WHERE ds.sessionId IN :sessionIds AND ds.tenantId = :tenantId")
    List<DialogueState> findBySessionIdInAndTenantId(@Param("sessionIds") List<String> sessionIds, @Param("tenantId") String tenantId);

    /**
     * Find dialogue states that need summarization (high token count or old).
     *
     * @param tenantId The tenant identifier
     * @param maxTokens Maximum tokens before summarization
     * @param maxAge Maximum age before summarization
     * @return List of dialogue states needing summarization
     */
    @Query("SELECT ds FROM DialogueState ds WHERE ds.tenantId = :tenantId AND (ds.cumulativeTokens > :maxTokens OR ds.lastUpdatedAt < :maxAge)")
    List<DialogueState> findNeedingSummarization(@Param("tenantId") String tenantId, @Param("maxTokens") int maxTokens, @Param("maxAge") LocalDateTime maxAge);

    /**
     * Count dialogue states for a user and tenant.
     *
     * @param userId The user identifier
     * @param tenantId The tenant identifier
     * @return Count of dialogue states
     */
    long countByUserIdAndTenantId(String userId, String tenantId);

    /**
     * Delete dialogue states older than a specified date for a tenant.
     *
     * @param tenantId The tenant identifier
     * @param cutoffDate The cutoff date
     * @return Number of deleted dialogue states
     */
    @Query("DELETE FROM DialogueState ds WHERE ds.tenantId = :tenantId AND ds.lastUpdatedAt < :cutoffDate")
    int deleteOldByTenantId(@Param("tenantId") String tenantId, @Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find dialogue states by topics for a tenant.
     *
     * @param topic The topic to search for
     * @param tenantId The tenant identifier
     * @param pageable Pagination parameters
     * @return Page of dialogue states
     */
    @Query("SELECT ds FROM DialogueState ds WHERE ds.tenantId = :tenantId AND ds.topics LIKE CONCAT('%', :topic, '%')")
    Page<DialogueState> findByTopicsContainingAndTenantId(@Param("topic") String topic, @Param("tenantId") String tenantId, Pageable pageable);
}

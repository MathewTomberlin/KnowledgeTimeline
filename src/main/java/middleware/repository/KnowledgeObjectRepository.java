package middleware.repository;

import middleware.model.KnowledgeObject;
import middleware.model.KnowledgeObjectType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for KnowledgeObject entity operations.
 * Provides data access methods for knowledge management.
 */
@Repository
public interface KnowledgeObjectRepository extends JpaRepository<KnowledgeObject, UUID> {

    /**
     * Find a knowledge object by its ID and tenant.
     *
     * @param id The knowledge object ID
     * @param tenantId The tenant identifier
     * @return Optional containing the knowledge object if found
     */
    Optional<KnowledgeObject> findByIdAndTenantId(UUID id, String tenantId);

    /**
     * Find all knowledge objects for a specific tenant.
     *
     * @param tenantId The tenant identifier
     * @param pageable Pagination parameters
     * @return Page of knowledge objects
     */
    Page<KnowledgeObject> findByTenantId(String tenantId, Pageable pageable);

    /**
     * Find knowledge objects by type and tenant.
     *
     * @param type The knowledge object type
     * @param tenantId The tenant identifier
     * @param pageable Pagination parameters
     * @return Page of knowledge objects
     */
    Page<KnowledgeObject> findByTypeAndTenantId(KnowledgeObjectType type, String tenantId, Pageable pageable);

    /**
     * Find knowledge objects by session ID and tenant.
     *
     * @param sessionId The session identifier
     * @param tenantId The tenant identifier
     * @return List of knowledge objects for the session
     */
    List<KnowledgeObject> findBySessionIdAndTenantId(String sessionId, String tenantId);

    /**
     * Find knowledge objects by user ID and tenant.
     *
     * @param userId The user identifier
     * @param tenantId The tenant identifier
     * @param pageable Pagination parameters
     * @return Page of knowledge objects
     */
    Page<KnowledgeObject> findByUserIdAndTenantId(String userId, String tenantId, Pageable pageable);

    /**
     * Find knowledge objects by parent ID and tenant.
     *
     * @param parentId The parent knowledge object ID
     * @param tenantId The tenant identifier
     * @return List of child knowledge objects
     */
    List<KnowledgeObject> findByParentIdAndTenantId(UUID parentId, String tenantId);

    /**
     * Find knowledge objects by tags and tenant.
     *
     * @param tags The tags to search for
     * @param tenantId The tenant identifier
     * @param pageable Pagination parameters
     * @return Page of knowledge objects
     */
    @Query("SELECT ko FROM KnowledgeObject ko WHERE ko.tenantId = :tenantId AND :tag MEMBER OF ko.tags")
    Page<KnowledgeObject> findByTagsContainingAndTenantId(@Param("tag") String tag, @Param("tenantId") String tenantId, Pageable pageable);

    /**
     * Find knowledge objects created within a date range for a tenant.
     *
     * @param tenantId The tenant identifier
     * @param startDate The start date
     * @param endDate The end date
     * @param pageable Pagination parameters
     * @return Page of knowledge objects
     */
    Page<KnowledgeObject> findByTenantIdAndCreatedAtBetween(String tenantId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Find non-archived knowledge objects for a tenant.
     *
     * @param tenantId The tenant identifier
     * @param pageable Pagination parameters
     * @return Page of non-archived knowledge objects
     */
    Page<KnowledgeObject> findByTenantIdAndArchivedFalse(String tenantId, Pageable pageable);

    /**
     * Count knowledge objects by type and tenant.
     *
     * @param type The knowledge object type
     * @param tenantId The tenant identifier
     * @return Count of knowledge objects
     */
    long countByTypeAndTenantId(KnowledgeObjectType type, String tenantId);

    /**
     * Find knowledge objects by multiple IDs and tenant.
     *
     * @param ids List of knowledge object IDs
     * @param tenantId The tenant identifier
     * @return List of knowledge objects
     */
    @Query("SELECT ko FROM KnowledgeObject ko WHERE ko.id IN :ids AND ko.tenantId = :tenantId")
    List<KnowledgeObject> findByIdInAndTenantId(@Param("ids") List<UUID> ids, @Param("tenantId") String tenantId);
}

package middleware.repository;

import middleware.model.ContentVariant;
import middleware.model.ContentVariantType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for ContentVariant entity operations.
 * Provides data access methods for content variant management.
 */
@Repository
public interface ContentVariantRepository extends JpaRepository<ContentVariant, UUID> {

    /**
     * Find a content variant by its ID and tenant.
     *
     * @param id The content variant ID
     * @param tenantId The tenant identifier
     * @return Optional containing the content variant if found
     */
    Optional<ContentVariant> findByIdAndTenantId(UUID id, String tenantId);

    /**
     * Find all content variants for a knowledge object.
     *
     * @param knowledgeObjectId The knowledge object ID
     * @param tenantId The tenant identifier
     * @return List of content variants
     */
    List<ContentVariant> findByKnowledgeObjectIdAndTenantId(UUID knowledgeObjectId, String tenantId);

    /**
     * Find content variants by type for a knowledge object.
     *
     * @param knowledgeObjectId The knowledge object ID
     * @param type The content variant type
     * @param tenantId The tenant identifier
     * @return List of content variants
     */
    List<ContentVariant> findByKnowledgeObjectIdAndTypeAndTenantId(UUID knowledgeObjectId, ContentVariantType type, String tenantId);

    /**
     * Find a specific content variant by knowledge object and type.
     *
     * @param knowledgeObjectId The knowledge object ID
     * @param type The content variant type
     * @param tenantId The tenant identifier
     * @return Optional containing the content variant if found
     */
    Optional<ContentVariant> findByKnowledgeObjectIdAndTypeAndTenantIdOrderByCreatedAtDesc(UUID knowledgeObjectId, ContentVariantType type, String tenantId);

    /**
     * Find content variants by embedding ID.
     *
     * @param embeddingId The embedding identifier
     * @param tenantId The tenant identifier
     * @return Optional containing the content variant if found
     */
    Optional<ContentVariant> findByEmbeddingIdAndTenantId(String embeddingId, String tenantId);

    /**
     * Find content variants that have embeddings.
     *
     * @param tenantId The tenant identifier
     * @return List of content variants with embeddings
     */
    @Query("SELECT cv FROM ContentVariant cv WHERE cv.tenantId = :tenantId AND cv.embeddingId IS NOT NULL")
    List<ContentVariant> findWithEmbeddingsByTenantId(@Param("tenantId") String tenantId);

    /**
     * Find content variants by storage URI.
     *
     * @param storageUri The storage URI
     * @param tenantId The tenant identifier
     * @return Optional containing the content variant if found
     */
    Optional<ContentVariant> findByStorageUriAndTenantId(String storageUri, String tenantId);

    /**
     * Find content variants that have content stored in blob storage.
     *
     * @param tenantId The tenant identifier
     * @return List of content variants with blob storage
     */
    @Query("SELECT cv FROM ContentVariant cv WHERE cv.tenantId = :tenantId AND cv.storageUri IS NOT NULL")
    List<ContentVariant> findWithBlobStorageByTenantId(@Param("tenantId") String tenantId);

    /**
     * Find content variants by multiple knowledge object IDs.
     *
     * @param knowledgeObjectIds List of knowledge object IDs
     * @param tenantId The tenant identifier
     * @return List of content variants
     */
    @Query("SELECT cv FROM ContentVariant cv WHERE cv.knowledgeObjectId IN :knowledgeObjectIds AND cv.tenantId = :tenantId")
    List<ContentVariant> findByKnowledgeObjectIdInAndTenantId(@Param("knowledgeObjectIds") List<UUID> knowledgeObjectIds, @Param("tenantId") String tenantId);

    /**
     * Count content variants by type for a knowledge object.
     *
     * @param knowledgeObjectId The knowledge object ID
     * @param type The content variant type
     * @param tenantId The tenant identifier
     * @return Count of content variants
     */
    long countByKnowledgeObjectIdAndTypeAndTenantId(UUID knowledgeObjectId, ContentVariantType type, String tenantId);
}

package middleware.repository;

import middleware.model.ContentVariant;
import middleware.model.ContentVariantType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ContentVariant entity operations.
 * Provides data access methods for content variant management.
 */
@Repository
public interface ContentVariantRepository extends JpaRepository<ContentVariant, String> {

    /**
     * Find a content variant by its ID.
     *
     * @param id The content variant ID
     * @return Optional containing the content variant if found
     */
    Optional<ContentVariant> findById(String id);

    /**
     * Find all content variants for a knowledge object.
     *
     * @param knowledgeObjectId The knowledge object ID
     * @return List of content variants
     */
    List<ContentVariant> findByKnowledgeObjectId(String knowledgeObjectId);

    /**
     * Find content variants by type for a knowledge object.
     *
     * @param knowledgeObjectId The knowledge object ID
     * @param type The content variant type
     * @return List of content variants
     */
    List<ContentVariant> findByKnowledgeObjectIdAndVariant(String knowledgeObjectId, ContentVariantType type);

    /**
     * Find a specific content variant by knowledge object and type.
     *
     * @param knowledgeObjectId The knowledge object ID
     * @param type The content variant type
     * @return Optional containing the content variant if found
     */
    Optional<ContentVariant> findByKnowledgeObjectIdAndVariantOrderByCreatedAtDesc(String knowledgeObjectId, ContentVariantType type);

    /**
     * Find content variants by embedding ID.
     *
     * @param embeddingId The embedding identifier
     * @return Optional containing the content variant if found
     */
    Optional<ContentVariant> findByEmbeddingId(String embeddingId);

    /**
     * Find content variants that have embeddings.
     *
     * @param tenantId The tenant identifier
     * @return List of content variants with embeddings
     */
    @Query("SELECT cv FROM ContentVariant cv JOIN KnowledgeObject ko ON cv.knowledgeObjectId = ko.id WHERE ko.tenantId = :tenantId AND cv.embeddingId IS NOT NULL")
    List<ContentVariant> findWithEmbeddingsByTenantId(@Param("tenantId") String tenantId);

    /**
     * Find content variants by storage URI.
     *
     * @param storageUri The storage URI
     * @return Optional containing the content variant if found
     */
    Optional<ContentVariant> findByStorageUri(String storageUri);

    /**
     * Find content variants that have content stored in blob storage.
     *
     * @param tenantId The tenant identifier
     * @return List of content variants with blob storage
     */
    @Query("SELECT cv FROM ContentVariant cv JOIN KnowledgeObject ko ON cv.knowledgeObjectId = ko.id WHERE ko.tenantId = :tenantId AND cv.storageUri IS NOT NULL")
    List<ContentVariant> findWithBlobStorageByTenantId(@Param("tenantId") String tenantId);

    /**
     * Find content variants by multiple knowledge object IDs.
     *
     * @param knowledgeObjectIds List of knowledge object IDs
     * @return List of content variants
     */
    @Query("SELECT cv FROM ContentVariant cv WHERE cv.knowledgeObjectId IN :knowledgeObjectIds")
    List<ContentVariant> findByKnowledgeObjectIdIn(@Param("knowledgeObjectIds") List<String> knowledgeObjectIds);

    /**
     * Count content variants by type for a knowledge object.
     *
     * @param knowledgeObjectId The knowledge object ID
     * @param type The content variant type
     * @return Count of content variants
     */
    long countByKnowledgeObjectIdAndVariant(String knowledgeObjectId, ContentVariantType type);
}

package middleware.repository;

import middleware.model.KnowledgeRelationship;
import middleware.model.RelationshipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for KnowledgeRelationship entity operations.
 * Provides data access methods for relationship management.
 */
@Repository
public interface KnowledgeRelationshipRepository extends JpaRepository<KnowledgeRelationship, UUID> {

    /**
     * Find relationships by source object ID.
     *
     * @param sourceId The source knowledge object ID
     * @return List of relationships
     */
    List<KnowledgeRelationship> findBySourceId(UUID sourceId);

    /**
     * Find relationships by target object ID.
     *
     * @param targetId The target knowledge object ID
     * @return List of relationships
     */
    List<KnowledgeRelationship> findByTargetId(UUID targetId);

    /**
     * Find relationships by type.
     *
     * @param type The relationship type
     * @return List of relationships
     */
    List<KnowledgeRelationship> findByType(RelationshipType type);

    /**
     * Find relationships by source and type.
     *
     * @param sourceId The source knowledge object ID
     * @param type The relationship type
     * @return List of relationships
     */
    List<KnowledgeRelationship> findBySourceIdAndType(UUID sourceId, RelationshipType type);

    /**
     * Find relationships by target and type.
     *
     * @param targetId The target knowledge object ID
     * @param type The relationship type
     * @return List of relationships
     */
    List<KnowledgeRelationship> findByTargetIdAndType(UUID targetId, RelationshipType type);

    /**
     * Find relationships between two specific objects.
     *
     * @param sourceId The source knowledge object ID
     * @param targetId The target knowledge object ID
     * @return List of relationships
     */
    List<KnowledgeRelationship> findBySourceIdAndTargetId(UUID sourceId, UUID targetId);

    /**
     * Find relationships by confidence threshold.
     *
     * @param minConfidence Minimum confidence threshold
     * @return List of relationships
     */
    @Query("SELECT kr FROM KnowledgeRelationship kr WHERE kr.confidence >= :minConfidence")
    List<KnowledgeRelationship> findByConfidenceGreaterThanEqual(@Param("minConfidence") double minConfidence);

    /**
     * Find relationships by detected by field.
     *
     * @param detectedBy The detection method
     * @return List of relationships
     */
    List<KnowledgeRelationship> findByDetectedBy(String detectedBy);

    /**
     * Find relationships created within a date range.
     *
     * @param startDate The start date
     * @param endDate The end date
     * @return List of relationships
     */
    List<KnowledgeRelationship> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find relationships created before a specific date.
     *
     * @param date The cutoff date
     * @return List of relationships
     */
    List<KnowledgeRelationship> findByCreatedAtBefore(LocalDateTime date);

    /**
     * Delete relationships created before a specific date.
     *
     * @param date The cutoff date
     * @return Number of relationships deleted
     */
    int deleteByCreatedAtBefore(LocalDateTime date);

    /**
     * Count relationships by type.
     *
     * @param type The relationship type
     * @return Count of relationships
     */
    long countByType(RelationshipType type);

    /**
     * Count relationships by source object.
     *
     * @param sourceId The source knowledge object ID
     * @return Count of relationships
     */
    long countBySourceId(UUID sourceId);

    /**
     * Count relationships by target object.
     *
     * @param targetId The target knowledge object ID
     * @return Count of relationships
     */
    long countByTargetId(UUID targetId);

    /**
     * Find relationships with highest confidence for a source object.
     *
     * @param sourceId The source knowledge object ID
     * @return Optional containing the relationship with highest confidence
     */
    @Query("SELECT kr FROM KnowledgeRelationship kr WHERE kr.sourceId = :sourceId ORDER BY kr.confidence DESC")
    Optional<KnowledgeRelationship> findTopBySourceIdOrderByConfidenceDesc(@Param("sourceId") UUID sourceId);

    /**
     * Find relationships with highest confidence for a target object.
     *
     * @param targetId The target knowledge object ID
     * @return Optional containing the relationship with highest confidence
     */
    @Query("SELECT kr FROM KnowledgeRelationship kr WHERE kr.targetId = :targetId ORDER BY kr.confidence DESC")
    Optional<KnowledgeRelationship> findTopByTargetIdOrderByConfidenceDesc(@Param("targetId") UUID targetId);
}

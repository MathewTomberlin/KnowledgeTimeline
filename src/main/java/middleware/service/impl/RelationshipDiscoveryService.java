package middleware.service.impl;

import middleware.model.KnowledgeObject;
import middleware.model.KnowledgeRelationship;
import middleware.model.RelationshipType;
import middleware.repository.KnowledgeObjectRepository;
import middleware.repository.KnowledgeRelationshipRepository;
import middleware.service.VectorStoreService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for discovering relationships between knowledge objects.
 * Uses vector similarity and semantic analysis to find connections.
 */
@Service
public class RelationshipDiscoveryService {

    private final KnowledgeObjectRepository knowledgeObjectRepository;
    private final KnowledgeRelationshipRepository relationshipRepository;
    private final VectorStoreService vectorStoreService;

    public RelationshipDiscoveryService(KnowledgeObjectRepository knowledgeObjectRepository,
                                      KnowledgeRelationshipRepository relationshipRepository,
                                      VectorStoreService vectorStoreService) {
        this.knowledgeObjectRepository = knowledgeObjectRepository;
        this.relationshipRepository = relationshipRepository;
        this.vectorStoreService = vectorStoreService;
    }

    /**
     * Discover relationships for a specific knowledge object.
     *
     * @param objectId The knowledge object ID to analyze
     * @param tenantId The tenant identifier
     * @return Number of relationships discovered
     */
    public int discoverRelationshipsForObject(String objectId, String tenantId) {
        try {
            // Find the source knowledge object
            var sourceObject = knowledgeObjectRepository.findByIdAndTenantId(objectId, tenantId);
            if (sourceObject.isEmpty()) {
                return 0;
            }

            KnowledgeObject source = sourceObject.get();
            
            // Find similar objects using vector search
            List<VectorStoreService.SimilarityMatch> similarObjects = vectorStoreService.findSimilar(
                source.getMetadata(), // Use metadata as search text
                10, // Find top 10 similar objects
                Map.of("tenantId", tenantId, "type", source.getType().toString()),
                true, // Use MMR for diversity
                0.3 // Diversity parameter
            );

            int relationshipsCreated = 0;
            for (VectorStoreService.SimilarityMatch match : similarObjects) {
                // Skip if it's the same object
                if (match.getObjectId().equals(objectId)) {
                    continue;
                }

                // Determine relationship type based on similarity score
                RelationshipType relationshipType = determineRelationshipType(match.getSimilarityScore());
                
                // Create relationship
                KnowledgeRelationship relationship = new KnowledgeRelationship();
                relationship.setSourceId(objectId);
                relationship.setTargetId(match.getObjectId());
                relationship.setType(relationshipType);
                relationship.setConfidence(match.getSimilarityScore());
                relationship.setEvidence("Vector similarity: " + match.getSimilarityScore());
                relationship.setDetectedBy("RelationshipDiscoveryService");
                relationship.setCreatedAt(LocalDateTime.now());

                relationshipRepository.save(relationship);
                relationshipsCreated++;
            }

            return relationshipsCreated;
        } catch (Exception e) {
            // Log error and return 0
            return 0;
        }
    }

    /**
     * Discover relationships for all knowledge objects in a tenant.
     *
     * @param tenantId The tenant identifier
     * @return Total number of relationships discovered
     */
    public int discoverRelationshipsForTenant(String tenantId) {
        try {
            // Get all knowledge objects for the tenant
            var knowledgeObjects = knowledgeObjectRepository.findByTenantId(tenantId, null);
            
            int totalRelationships = 0;
            for (KnowledgeObject object : knowledgeObjects) {
                totalRelationships += discoverRelationshipsForObject(object.getId().toString(), tenantId);
            }
            
            return totalRelationships;
        } catch (Exception e) {
            // Log error and return 0
            return 0;
        }
    }

    /**
     * Determine relationship type based on similarity score.
     *
     * @param similarityScore The similarity score (0.0 to 1.0)
     * @return The relationship type
     */
    private RelationshipType determineRelationshipType(double similarityScore) {
        if (similarityScore > 0.8) {
            return RelationshipType.SUPPORTS; // High similarity suggests supporting relationship
        } else if (similarityScore > 0.6) {
            return RelationshipType.REFERENCES; // Medium similarity suggests reference relationship
        } else if (similarityScore > 0.4) {
            return RelationshipType.CONTRADICTS; // Lower similarity might indicate contradiction
        } else {
            return RelationshipType.REFERENCES; // Default to reference for low similarity
        }
    }

    /**
     * Clean up old relationships for a tenant.
     *
     * @param tenantId The tenant identifier
     * @param olderThanDays Remove relationships older than this many days
     * @return Number of relationships removed
     */
    public int cleanupOldRelationships(String tenantId, int olderThanDays) {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(olderThanDays);
            // For now, just delete all relationships created before the cutoff date
            // TODO: Add tenant filtering when KnowledgeRelationship entity supports it
            return relationshipRepository.deleteByCreatedAtBefore(cutoffDate);
        } catch (Exception e) {
            // Log error and return 0
            return 0;
        }
    }
}

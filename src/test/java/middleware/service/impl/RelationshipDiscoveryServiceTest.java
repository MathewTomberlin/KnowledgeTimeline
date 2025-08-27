package middleware.service.impl;

import middleware.model.KnowledgeObject;
import middleware.model.KnowledgeObjectType;
import middleware.model.KnowledgeRelationship;
import middleware.model.RelationshipType;
import middleware.repository.KnowledgeObjectRepository;
import middleware.repository.KnowledgeRelationshipRepository;
import middleware.service.VectorStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for RelationshipDiscoveryService implementation.
 */
@ExtendWith(MockitoExtension.class)
class RelationshipDiscoveryServiceTest {

    @Mock
    private KnowledgeObjectRepository knowledgeObjectRepository;

    @Mock
    private KnowledgeRelationshipRepository relationshipRepository;

    @Mock
    private VectorStoreService vectorStoreService;

    private RelationshipDiscoveryService relationshipDiscoveryService;

    private KnowledgeObject testKnowledgeObject;
    private List<VectorStoreService.SimilarityMatch> mockSimilarityMatches;

    @BeforeEach
    void setUp() {
        // Manually instantiate the service with mocked dependencies
        relationshipDiscoveryService = new RelationshipDiscoveryService(
            knowledgeObjectRepository,
            relationshipRepository,
            vectorStoreService
        );
        
        // Create test knowledge object
        testKnowledgeObject = new KnowledgeObject();
        testKnowledgeObject.setId("obj1-0000-0000-0000-000000000000");
        testKnowledgeObject.setTenantId("tenant1");
        testKnowledgeObject.setType(KnowledgeObjectType.FILE_CHUNK);
        testKnowledgeObject.setMetadata("Sample document metadata");

        // Create mock similarity matches with valid UUID strings
        // These should not conflict with the test object IDs
        mockSimilarityMatches = Arrays.asList(
            new VectorStoreService.SimilarityMatch("obj4-0000-0000-0000-000000000000", "var4", 0.85, "text4", new HashMap<>()),
            new VectorStoreService.SimilarityMatch("obj5-0000-0000-0000-000000000000", "var5", 0.75, "text5", new HashMap<>()),
            new VectorStoreService.SimilarityMatch("obj6-0000-0000-0000-000000000000", "var6", 0.65, "text6", new HashMap<>()),
            new VectorStoreService.SimilarityMatch("obj7-0000-0000-0000-000000000000", "var7", 0.55, "text7", new HashMap<>())
        );
    }

    @Test
    void testDiscoverRelationshipsForObject_WithValidObject() {
        // Given
        String objectId = testKnowledgeObject.getId().toString();
        String tenantId = "tenant1";

        when(knowledgeObjectRepository.findByIdAndTenantId(objectId, tenantId))
            .thenReturn(Optional.of(testKnowledgeObject));
        when(vectorStoreService.findSimilar(
            anyString(),
            anyInt(),
            anyMap(),
            anyBoolean(),
            anyDouble()
        )).thenReturn(mockSimilarityMatches);
        when(relationshipRepository.save(any(KnowledgeRelationship.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        int result = relationshipDiscoveryService.discoverRelationshipsForObject(objectId, tenantId);

        // Then
        assertEquals(4, result);
        
        // Verify that the service was called correctly
        verify(knowledgeObjectRepository).findByIdAndTenantId(objectId, tenantId);
        verify(vectorStoreService).findSimilar(
            eq(testKnowledgeObject.getMetadata()),
            eq(10),
            argThat(filters -> 
                filters.containsKey("tenantId") && 
                filters.get("tenantId").equals(tenantId) &&
                filters.containsKey("type") &&
                filters.get("type").equals(testKnowledgeObject.getType().toString())
            ),
            eq(true),
            eq(0.3)
        );
        verify(relationshipRepository, times(4)).save(any(KnowledgeRelationship.class));
    }

    @Test
    void testDiscoverRelationshipsForObject_WithNonExistentObject() {
        // Given
        String objectId = "non-existent-id";
        String tenantId = "tenant1";

        when(knowledgeObjectRepository.findByIdAndTenantId(objectId, tenantId))
            .thenReturn(Optional.empty());

        // When
        int result = relationshipDiscoveryService.discoverRelationshipsForObject(objectId, tenantId);

        // Then
        assertEquals(0, result);
        
        verify(knowledgeObjectRepository).findByIdAndTenantId(objectId, tenantId);
        verify(vectorStoreService, never()).findSimilar(anyString(), anyInt(), anyMap(), anyBoolean(), anyDouble());
        verify(relationshipRepository, never()).save(any(KnowledgeRelationship.class));
    }

    @Test
    void testDiscoverRelationshipsForObject_WithSameObjectInResults() {
        // Given
        String objectId = testKnowledgeObject.getId().toString();
        String tenantId = "tenant1";

        // Create similarity matches that include the same object
        List<VectorStoreService.SimilarityMatch> matchesWithSameObject = Arrays.asList(
            new VectorStoreService.SimilarityMatch(objectId, "var1", 0.9, "text1", new HashMap<>()), // Same object
            new VectorStoreService.SimilarityMatch("obj2-0000-0000-0000-000000000000", "var2", 0.8, "text2", new HashMap<>())
        );

        when(knowledgeObjectRepository.findByIdAndTenantId(objectId, tenantId))
            .thenReturn(Optional.of(testKnowledgeObject));
        when(vectorStoreService.findSimilar(anyString(), anyInt(), anyMap(), anyBoolean(), anyDouble()))
            .thenReturn(matchesWithSameObject);
        when(relationshipRepository.save(any(KnowledgeRelationship.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        int result = relationshipDiscoveryService.discoverRelationshipsForObject(objectId, tenantId);

        // Then
        assertEquals(1, result); // Only one relationship should be created (excluding same object)
        
        verify(relationshipRepository, times(1)).save(any(KnowledgeRelationship.class));
    }

    @Test
    void testDiscoverRelationshipsForObject_WithException() {
        // Given
        String objectId = testKnowledgeObject.getId().toString();
        String tenantId = "tenant1";

        when(knowledgeObjectRepository.findByIdAndTenantId(objectId, tenantId))
            .thenThrow(new RuntimeException("Database error"));

        // When
        int result = relationshipDiscoveryService.discoverRelationshipsForObject(objectId, tenantId);

        // Then
        assertEquals(0, result);
    }

    @Test
    void testDiscoverRelationshipsForObject_RelationshipTypeDetermination() {
        // Given
        String objectId = testKnowledgeObject.getId().toString();
        String tenantId = "tenant1";

        // Create similarity matches with different scores to test relationship type determination
        // These should not include the same object ID as the source object
        List<VectorStoreService.SimilarityMatch> diverseMatches = Arrays.asList(
            new VectorStoreService.SimilarityMatch("obj5-0000-0000-0000-000000000000", "var5", 0.9, "text5", new HashMap<>()), // Should be SUPPORTS
            new VectorStoreService.SimilarityMatch("obj6-0000-0000-0000-000000000000", "var6", 0.7, "text6", new HashMap<>()), // Should be REFERENCES
            new VectorStoreService.SimilarityMatch("obj7-0000-0000-0000-000000000000", "var7", 0.5, "text7", new HashMap<>()), // Should be CONTRADICTS
            new VectorStoreService.SimilarityMatch("obj8-0000-0000-0000-000000000000", "var8", 0.3, "text8", new HashMap<>())  // Should be REFERENCES (default)
        );

        when(knowledgeObjectRepository.findByIdAndTenantId(objectId, tenantId))
            .thenReturn(Optional.of(testKnowledgeObject));
        when(vectorStoreService.findSimilar(anyString(), anyInt(), anyMap(), anyBoolean(), anyDouble()))
            .thenReturn(diverseMatches);
        when(relationshipRepository.save(any(KnowledgeRelationship.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        int result = relationshipDiscoveryService.discoverRelationshipsForObject(objectId, tenantId);

        // Then
        assertEquals(4, result);
        
        // Verify that relationships are saved with correct types
        verify(relationshipRepository, times(4)).save(argThat(relationship -> {
            if (relationship.getTargetId().toString().equals("obj5-0000-0000-0000-000000000000")) {
                return relationship.getType() == RelationshipType.SUPPORTS;
            } else if (relationship.getTargetId().toString().equals("obj6-0000-0000-0000-000000000000")) {
                return relationship.getType() == RelationshipType.REFERENCES;
            } else if (relationship.getTargetId().toString().equals("obj7-0000-0000-0000-000000000000")) {
                return relationship.getType() == RelationshipType.CONTRADICTS;
            } else if (relationship.getTargetId().toString().equals("obj8-0000-0000-0000-000000000000")) {
                return relationship.getType() == RelationshipType.REFERENCES;
            }
            return false;
        }));
    }

    @Test
    void testDiscoverRelationshipsForTenant_WithValidTenant() {
        // Given
        String tenantId = "tenant1";
        List<KnowledgeObject> tenantObjects = Arrays.asList(
            testKnowledgeObject,
            createKnowledgeObject("obj2", tenantId),
            createKnowledgeObject("obj3", tenantId)
        );

        when(knowledgeObjectRepository.findByTenantId(tenantId, null))
            .thenReturn(new PageImpl<>(tenantObjects));
        
        // Mock findByIdAndTenantId to return different objects for different IDs
        when(knowledgeObjectRepository.findByIdAndTenantId(eq("obj1-0000-0000-0000-000000000000"), eq(tenantId)))
            .thenReturn(Optional.of(tenantObjects.get(0)));
        when(knowledgeObjectRepository.findByIdAndTenantId(eq("obj2-0000-0000-0000-000000000000"), eq(tenantId)))
            .thenReturn(Optional.of(tenantObjects.get(1)));
        when(knowledgeObjectRepository.findByIdAndTenantId(eq("obj3-0000-0000-0000-000000000000"), eq(tenantId)))
            .thenReturn(Optional.of(tenantObjects.get(2)));
        
        // Mock vectorStoreService to return different similarity matches for each object
        // Each object should get 4 similarity matches that don't include itself
        when(vectorStoreService.findSimilar(anyString(), anyInt(), anyMap(), anyBoolean(), anyDouble()))
            .thenReturn(Arrays.asList(
                new VectorStoreService.SimilarityMatch("obj4-0000-0000-0000-000000000000", "var4", 0.85, "text4", new HashMap<>()),
                new VectorStoreService.SimilarityMatch("obj5-0000-0000-0000-000000000000", "var5", 0.75, "text5", new HashMap<>()),
                new VectorStoreService.SimilarityMatch("obj6-0000-0000-0000-000000000000", "var6", 0.65, "text6", new HashMap<>()),
                new VectorStoreService.SimilarityMatch("obj7-0000-0000-0000-000000000000", "var7", 0.55, "text7", new HashMap<>())
            ));
        when(relationshipRepository.save(any(KnowledgeRelationship.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        int result = relationshipDiscoveryService.discoverRelationshipsForTenant(tenantId);

        // Then
        assertEquals(12, result); // 3 objects × 4 relationships each
        
        verify(knowledgeObjectRepository).findByTenantId(tenantId, null);
        verify(knowledgeObjectRepository, times(3)).findByIdAndTenantId(anyString(), eq(tenantId));
        verify(vectorStoreService, times(3)).findSimilar(anyString(), anyInt(), anyMap(), anyBoolean(), anyDouble());
        verify(relationshipRepository, times(12)).save(any(KnowledgeRelationship.class));
    }

    @Test
    void testDiscoverRelationshipsForTenant_WithEmptyTenant() {
        // Given
        String tenantId = "tenant1";

        when(knowledgeObjectRepository.findByTenantId(tenantId, null))
            .thenReturn(new PageImpl<>(Collections.emptyList()));

        // When
        int result = relationshipDiscoveryService.discoverRelationshipsForTenant(tenantId);

        // Then
        assertEquals(0, result);
        
        verify(knowledgeObjectRepository).findByTenantId(tenantId, null);
        verify(knowledgeObjectRepository, never()).findByIdAndTenantId(anyString(), anyString());
        verify(vectorStoreService, never()).findSimilar(anyString(), anyInt(), anyMap(), anyBoolean(), anyDouble());
    }

    @Test
    void testDiscoverRelationshipsForTenant_WithException() {
        // Given
        String tenantId = "tenant1";

        when(knowledgeObjectRepository.findByTenantId(tenantId, null))
            .thenThrow(new RuntimeException("Database error"));

        // When
        int result = relationshipDiscoveryService.discoverRelationshipsForTenant(tenantId);

        // Then
        assertEquals(0, result);
    }

    @Test
    void testCleanupOldRelationships_WithValidParameters() {
        // Given
        String tenantId = "tenant1";
        int olderThanDays = 30;
        int expectedDeletedCount = 5;

        when(relationshipRepository.deleteByCreatedAtBefore(any(LocalDateTime.class)))
            .thenReturn(expectedDeletedCount);

        // When
        int result = relationshipDiscoveryService.cleanupOldRelationships(tenantId, olderThanDays);

        // Then
        assertEquals(expectedDeletedCount, result);
        
        verify(relationshipRepository).deleteByCreatedAtBefore(argThat(cutoffDate -> {
            LocalDateTime expectedCutoff = LocalDateTime.now().minusDays(olderThanDays);
            // Allow for small time differences (within 1 second)
            return Math.abs(cutoffDate.toEpochSecond(java.time.ZoneOffset.UTC) - 
                           expectedCutoff.toEpochSecond(java.time.ZoneOffset.UTC)) <= 1;
        }));
    }

    @Test
    void testCleanupOldRelationships_WithException() {
        // Given
        String tenantId = "tenant1";
        int olderThanDays = 30;

        when(relationshipRepository.deleteByCreatedAtBefore(any(LocalDateTime.class)))
            .thenThrow(new RuntimeException("Database error"));

        // When
        int result = relationshipDiscoveryService.cleanupOldRelationships(tenantId, olderThanDays);

        // Then
        assertEquals(0, result);
    }

    @Test
    void testCleanupOldRelationships_WithZeroDays() {
        // Given
        String tenantId = "tenant1";
        int olderThanDays = 0;
        int expectedDeletedCount = 0;

        when(relationshipRepository.deleteByCreatedAtBefore(any(LocalDateTime.class)))
            .thenReturn(expectedDeletedCount);

        // When
        int result = relationshipDiscoveryService.cleanupOldRelationships(tenantId, olderThanDays);

        // Then
        assertEquals(expectedDeletedCount, result);
        
        verify(relationshipRepository).deleteByCreatedAtBefore(argThat(cutoffDate -> {
            LocalDateTime expectedCutoff = LocalDateTime.now();
            // Allow for small time differences (within 1 second)
            return Math.abs(cutoffDate.toEpochSecond(java.time.ZoneOffset.UTC) - 
                           expectedCutoff.toEpochSecond(java.time.ZoneOffset.UTC)) <= 1;
        }));
    }

    @Test
    void testCleanupOldRelationships_WithNegativeDays() {
        // Given
        String tenantId = "tenant1";
        int olderThanDays = -5;
        int expectedDeletedCount = 0;

        when(relationshipRepository.deleteByCreatedAtBefore(any(LocalDateTime.class)))
            .thenReturn(expectedDeletedCount);

        // When
        int result = relationshipDiscoveryService.cleanupOldRelationships(tenantId, olderThanDays);

        // Then
        assertEquals(expectedDeletedCount, result);
        
        verify(relationshipRepository).deleteByCreatedAtBefore(argThat(cutoffDate -> {
            LocalDateTime expectedCutoff = LocalDateTime.now().minusDays(olderThanDays);
            // Allow for small time differences (within 1 second)
            return Math.abs(cutoffDate.toEpochSecond(java.time.ZoneOffset.UTC) - 
                           expectedCutoff.toEpochSecond(java.time.ZoneOffset.UTC)) <= 1;
        }));
    }

    @Test
    void testRelationshipCreation_WithValidData() {
        // Given
        String objectId = testKnowledgeObject.getId().toString();
        String tenantId = "tenant1";
        VectorStoreService.SimilarityMatch match = new VectorStoreService.SimilarityMatch(
            "obj2-0000-0000-0000-000000000000", "var2", 0.85, "text2", new HashMap<>()
        );

        when(knowledgeObjectRepository.findByIdAndTenantId(objectId, tenantId))
            .thenReturn(Optional.of(testKnowledgeObject));
        when(vectorStoreService.findSimilar(anyString(), anyInt(), anyMap(), anyBoolean(), anyDouble()))
            .thenReturn(Arrays.asList(match));
        when(relationshipRepository.save(any(KnowledgeRelationship.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        int result = relationshipDiscoveryService.discoverRelationshipsForObject(objectId, tenantId);

        // Then
        assertEquals(1, result);
        
        verify(relationshipRepository).save(argThat(relationship -> {
            return relationship.getSourceId().toString().equals(objectId) &&
                   relationship.getTargetId().toString().equals("obj2-0000-0000-0000-000000000000") &&
                   relationship.getType() == RelationshipType.SUPPORTS &&
                   relationship.getConfidence() == 0.85 &&
                   relationship.getEvidence().contains("Vector similarity: 0.85") &&
                   relationship.getDetectedBy().equals("RelationshipDiscoveryService") &&
                   relationship.getCreatedAt() != null;
        }));
    }

    // Helper method to create test knowledge objects
    private KnowledgeObject createKnowledgeObject(String id, String tenantId) {
        KnowledgeObject obj = new KnowledgeObject();
        obj.setId(id + "-0000-0000-0000-000000000000");
        obj.setTenantId(tenantId);
        obj.setType(KnowledgeObjectType.FILE_CHUNK);
        obj.setMetadata("Test metadata for " + id);
        return obj;
    }
}

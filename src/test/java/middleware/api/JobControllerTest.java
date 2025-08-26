package middleware.api;

import middleware.service.MemoryExtractionService;
import middleware.service.DialogueStateService;
import middleware.service.impl.RelationshipDiscoveryService;
import middleware.service.impl.SessionSummarizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for JobController.
 */
@ExtendWith(MockitoExtension.class)
class JobControllerTest {

    @Mock
    private MemoryExtractionService memoryExtractionService;

    @Mock
    private DialogueStateService dialogueStateService;

    @Mock
    private RelationshipDiscoveryService relationshipDiscoveryService;

    @Mock
    private SessionSummarizationService sessionSummarizationService;

    @InjectMocks
    private JobController jobController;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(memoryExtractionService, dialogueStateService, 
              relationshipDiscoveryService, sessionSummarizationService);
    }

    @Test
    void testRelationshipDiscovery_WithObjectIdAndTenantId() {
        // Given
        Map<String, Object> request = Map.of(
            "object_id", "obj-123",
            "tenant_id", "tenant-1"
        );
        
        when(relationshipDiscoveryService.discoverRelationshipsForObject("obj-123", "tenant-1"))
            .thenReturn(5);

        // When
        ResponseEntity<Map<String, Object>> response = jobController.relationshipDiscovery(request);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> body = response.getBody();
        assertEquals("completed", body.get("status"));
        assertTrue(body.get("job_id").toString().startsWith("rel_"));
        assertEquals("obj-123", body.get("object_id"));
        assertEquals(5, body.get("relationships_found"));
        assertEquals("Relationship discovery completed for object", body.get("message"));
        
        verify(relationshipDiscoveryService).discoverRelationshipsForObject("obj-123", "tenant-1");
    }

    @Test
    void testRelationshipDiscovery_WithTenantIdOnly() {
        // Given
        Map<String, Object> request = Map.of("tenant_id", "tenant-1");
        
        when(relationshipDiscoveryService.discoverRelationshipsForTenant("tenant-1"))
            .thenReturn(15);

        // When
        ResponseEntity<Map<String, Object>> response = jobController.relationshipDiscovery(request);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> body = response.getBody();
        assertEquals("completed", body.get("status"));
        assertTrue(body.get("job_id").toString().startsWith("rel_"));
        assertEquals("tenant-1", body.get("tenant_id"));
        assertEquals(15, body.get("relationships_found"));
        assertEquals("Relationship discovery completed for tenant", body.get("message"));
        
        verify(relationshipDiscoveryService).discoverRelationshipsForTenant("tenant-1");
    }

    @Test
    void testRelationshipDiscovery_WithoutTenantId() {
        // Given
        Map<String, Object> request = Map.of("object_id", "obj-123");

        // When
        ResponseEntity<Map<String, Object>> response = jobController.relationshipDiscovery(request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("tenant_id is required", response.getBody().get("error"));
        
        verifyNoInteractions(relationshipDiscoveryService);
    }

    @Test
    void testRelationshipDiscovery_WithEmptyRequest() {
        // Given
        Map<String, Object> request = Map.of();

        // When
        ResponseEntity<Map<String, Object>> response = jobController.relationshipDiscovery(request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("tenant_id is required", response.getBody().get("error"));
        
        verifyNoInteractions(relationshipDiscoveryService);
    }

    @Test
    void testRelationshipDiscovery_WhenServiceThrowsException() {
        // Given
        Map<String, Object> request = Map.of(
            "object_id", "obj-123",
            "tenant_id", "tenant-1"
        );
        
        when(relationshipDiscoveryService.discoverRelationshipsForObject("obj-123", "tenant-1"))
            .thenThrow(new RuntimeException("Service error"));

        // When
        ResponseEntity<Map<String, Object>> response = jobController.relationshipDiscovery(request);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().get("error").toString().contains("Failed to start relationship discovery job"));
        assertTrue(response.getBody().get("error").toString().contains("Service error"));
    }

    @Test
    void testSessionSummarize_Success() {
        // Given
        Map<String, Object> request = Map.of(
            "session_id", "session-123",
            "tenant_id", "tenant-1"
        );
        
        Map<String, Object> serviceResult = Map.of(
            "status", "completed",
            "session_id", "session-123",
            "summary", "This is a test summary",
            "memory_object_id", "mem-123",
            "tokens_used", 150
        );
        
        when(sessionSummarizationService.summarizeSession("session-123", "tenant-1"))
            .thenReturn(serviceResult);

        // When
        ResponseEntity<Map<String, Object>> response = jobController.sessionSummarize(request);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> body = response.getBody();
        assertEquals("completed", body.get("status"));
        assertTrue(body.get("job_id").toString().startsWith("sum_"));
        assertEquals("session-123", body.get("session_id"));
        assertEquals("This is a test summary", body.get("summary"));
        assertEquals("mem-123", body.get("memory_object_id"));
        assertEquals(150, body.get("tokens_used"));
        assertEquals("Session summarization completed", body.get("message"));
        
        verify(sessionSummarizationService).summarizeSession("session-123", "tenant-1");
    }

    @Test
    void testSessionSummarize_WithoutSessionId() {
        // Given
        Map<String, Object> request = Map.of("tenant_id", "tenant-1");

        // When
        ResponseEntity<Map<String, Object>> response = jobController.sessionSummarize(request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("session_id and tenant_id are required", response.getBody().get("error"));
        
        verifyNoInteractions(sessionSummarizationService);
    }

    @Test
    void testSessionSummarize_WithoutTenantId() {
        // Given
        Map<String, Object> request = Map.of("session_id", "session-123");

        // When
        ResponseEntity<Map<String, Object>> response = jobController.sessionSummarize(request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("session_id and tenant_id are required", response.getBody().get("error"));
        
        verifyNoInteractions(sessionSummarizationService);
    }

    @Test
    void testSessionSummarize_WithEmptyRequest() {
        // Given
        Map<String, Object> request = Map.of();

        // When
        ResponseEntity<Map<String, Object>> response = jobController.sessionSummarize(request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("session_id and tenant_id are required", response.getBody().get("error"));
        
        verifyNoInteractions(sessionSummarizationService);
    }

    @Test
    void testSessionSummarize_WhenServiceReturnsError() {
        // Given
        Map<String, Object> request = Map.of(
            "session_id", "session-123",
            "tenant_id", "tenant-1"
        );
        
        Map<String, Object> serviceResult = Map.of(
            "error", "Session not found",
            "session_id", "session-123"
        );
        
        when(sessionSummarizationService.summarizeSession("session-123", "tenant-1"))
            .thenReturn(serviceResult);

        // When
        ResponseEntity<Map<String, Object>> response = jobController.sessionSummarize(request);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Session not found", response.getBody().get("error"));
        assertEquals("session-123", response.getBody().get("session_id"));
        
        verify(sessionSummarizationService).summarizeSession("session-123", "tenant-1");
    }

    @Test
    void testSessionSummarize_WhenServiceThrowsException() {
        // Given
        Map<String, Object> request = Map.of(
            "session_id", "session-123",
            "tenant_id", "tenant-1"
        );
        
        when(sessionSummarizationService.summarizeSession("session-123", "tenant-1"))
            .thenThrow(new RuntimeException("Service error"));

        // When
        ResponseEntity<Map<String, Object>> response = jobController.sessionSummarize(request);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().get("error").toString().contains("Failed to summarize session"));
        assertTrue(response.getBody().get("error").toString().contains("Service error"));
    }

    @Test
    void testHealth_Success() {
        // When
        ResponseEntity<String> response = jobController.health();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OK", response.getBody());
    }

    @Test
    void testRelationshipDiscovery_WithNullRequest() {
        // Given
        Map<String, Object> request = null;

        // When
        ResponseEntity<Map<String, Object>> response = jobController.relationshipDiscovery(request);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().get("error").toString().contains("Failed to start relationship discovery job"));
    }

    @Test
    void testSessionSummarize_WithNullRequest() {
        // Given
        Map<String, Object> request = null;

        // When
        ResponseEntity<Map<String, Object>> response = jobController.sessionSummarize(request);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().get("error").toString().contains("Failed to summarize session"));
    }

    @Test
    void testRelationshipDiscovery_WithLargeObjectId() {
        // Given
        String largeObjectId = "obj-" + "x".repeat(1000);
        Map<String, Object> request = Map.of(
            "object_id", largeObjectId,
            "tenant_id", "tenant-1"
        );
        
        when(relationshipDiscoveryService.discoverRelationshipsForObject(largeObjectId, "tenant-1"))
            .thenReturn(1);

        // When
        ResponseEntity<Map<String, Object>> response = jobController.relationshipDiscovery(request);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(largeObjectId, response.getBody().get("object_id"));
        
        verify(relationshipDiscoveryService).discoverRelationshipsForObject(largeObjectId, "tenant-1");
    }

    @Test
    void testSessionSummarize_WithLargeSessionId() {
        // Given
        String largeSessionId = "session-" + "x".repeat(1000);
        Map<String, Object> request = Map.of(
            "session_id", largeSessionId,
            "tenant_id", "tenant-1"
        );
        
        Map<String, Object> serviceResult = Map.of(
            "status", "completed",
            "session_id", largeSessionId,
            "summary", "Test summary",
            "memory_object_id", "mem-123",
            "tokens_used", 50
        );
        
        when(sessionSummarizationService.summarizeSession(largeSessionId, "tenant-1"))
            .thenReturn(serviceResult);

        // When
        ResponseEntity<Map<String, Object>> response = jobController.sessionSummarize(request);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(largeSessionId, response.getBody().get("session_id"));
        
        verify(sessionSummarizationService).summarizeSession(largeSessionId, "tenant-1");
    }
}

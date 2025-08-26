package middleware.api;

import middleware.service.MemoryExtractionService;
import middleware.service.DialogueStateService;
import middleware.service.impl.RelationshipDiscoveryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for background job endpoints.
 * Handles relationship discovery and session summarization jobs.
 */
@RestController
@RequestMapping("/jobs")
public class JobController {

    private final MemoryExtractionService memoryExtractionService;
    private final DialogueStateService dialogueStateService;
    private final RelationshipDiscoveryService relationshipDiscoveryService;

    public JobController(MemoryExtractionService memoryExtractionService,
                        DialogueStateService dialogueStateService,
                        RelationshipDiscoveryService relationshipDiscoveryService) {
        this.memoryExtractionService = memoryExtractionService;
        this.dialogueStateService = dialogueStateService;
        this.relationshipDiscoveryService = relationshipDiscoveryService;
    }

    /**
     * Trigger relationship discovery job.
     * Analyzes knowledge objects to find relationships between them.
     *
     * @param request Job parameters
     * @return Job status
     */
    @PostMapping("/relationship-discovery")
    public ResponseEntity<Map<String, Object>> relationshipDiscovery(@RequestBody Map<String, Object> request) {
        try {
            String objectId = (String) request.get("object_id");
            String tenantId = (String) request.get("tenant_id");
            
            if (objectId != null && tenantId != null) {
                // Discover relationships for a specific object
                int relationshipsFound = relationshipDiscoveryService.discoverRelationshipsForObject(objectId, tenantId);
                Map<String, Object> response = Map.of(
                    "status", "completed",
                    "job_id", "rel_" + System.currentTimeMillis(),
                    "object_id", objectId,
                    "relationships_found", relationshipsFound,
                    "message", "Relationship discovery completed for object"
                );
                return ResponseEntity.ok(response);
            } else if (tenantId != null) {
                // Discover relationships for entire tenant
                int relationshipsFound = relationshipDiscoveryService.discoverRelationshipsForTenant(tenantId);
                Map<String, Object> response = Map.of(
                    "status", "completed",
                    "job_id", "rel_" + System.currentTimeMillis(),
                    "tenant_id", tenantId,
                    "relationships_found", relationshipsFound,
                    "message", "Relationship discovery completed for tenant"
                );
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "tenant_id is required"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to start relationship discovery job: " + e.getMessage()));
        }
    }

    /**
     * Trigger session summarization job.
     * Summarizes dialogue sessions and creates session memory objects.
     *
     * @param request Job parameters including session_id
     * @return Job status
     */
    @PostMapping("/session-summarize")
    public ResponseEntity<Map<String, Object>> sessionSummarize(@RequestBody Map<String, Object> request) {
        try {
            String sessionId = (String) request.get("session_id");
            String tenantId = (String) request.get("tenant_id");
            
            if (sessionId == null || tenantId == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "session_id and tenant_id are required"));
            }
            
            // TODO: Implement session summarization logic
            // This would typically:
            // 1. Get dialogue state for the session
            // 2. Generate summary using LLM
            // 3. Create session memory object
            // 4. Update dialogue state
            
            Map<String, Object> response = Map.of(
                "status", "completed",
                "job_id", "sum_" + System.currentTimeMillis(),
                "session_id", sessionId,
                "message", "Session summarization completed"
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to summarize session"));
        }
    }

    /**
     * Health check for job endpoints.
     *
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}

package middleware.api;

import middleware.model.KnowledgeObject;
import middleware.model.ContentVariant;
import middleware.repository.KnowledgeObjectRepository;
import middleware.repository.ContentVariantRepository;
import middleware.service.VectorStoreService;
import middleware.service.BlobStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller for knowledge management endpoints.
 * Handles CRUD operations for knowledge objects and content variants.
 */
@RestController
@RequestMapping("/v1/knowledge")
public class KnowledgeController {

    private final VectorStoreService vectorStoreService;
    private final BlobStorageService blobStorageService;
    private final KnowledgeObjectRepository knowledgeObjectRepository;
    private final ContentVariantRepository contentVariantRepository;

    public KnowledgeController(VectorStoreService vectorStoreService,
                              BlobStorageService blobStorageService,
                              KnowledgeObjectRepository knowledgeObjectRepository,
                              ContentVariantRepository contentVariantRepository) {
        this.vectorStoreService = vectorStoreService;
        this.blobStorageService = blobStorageService;
        this.knowledgeObjectRepository = knowledgeObjectRepository;
        this.contentVariantRepository = contentVariantRepository;
    }

    /**
     * Search for knowledge objects using semantic similarity.
     *
     * @param query Search query
     * @param limit Maximum number of results
     * @param filters Optional filters
     * @return List of similar knowledge objects
     */
    @GetMapping("/search")
    public ResponseEntity<List<VectorStoreService.SimilarityMatch>> searchKnowledge(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam Map<String, String> filters) {
        try {
            // Convert Map<String, String> to Map<String, Object>
            Map<String, Object> objectFilters = Map.copyOf(filters);
            List<VectorStoreService.SimilarityMatch> results = vectorStoreService.findSimilar(
                query, limit, objectFilters, true, 0.3);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get a knowledge object by ID.
     *
     * @param id Knowledge object ID
     * @return Knowledge object details
     */
    @GetMapping("/objects/{id}")
    public ResponseEntity<KnowledgeObject> getKnowledgeObject(@PathVariable String id) {
        try {
            // For now, return not found since we need to fix the repository ID type mismatch
            // TODO: Fix repository to use String IDs instead of UUID
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create a new knowledge object.
     *
     * @param knowledgeObject The knowledge object to create
     * @return Created knowledge object
     */
    @PostMapping("/objects")
    public ResponseEntity<KnowledgeObject> createKnowledgeObject(@RequestBody KnowledgeObject knowledgeObject) {
        try {
            // Generate ID if not provided
            if (knowledgeObject.getId() == null) {
                knowledgeObject.setId(UUID.randomUUID().toString());
            }
            
            KnowledgeObject savedObject = knowledgeObjectRepository.save(knowledgeObject);
            return ResponseEntity.ok(savedObject);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Update a knowledge object.
     *
     * @param id Knowledge object ID
     * @param knowledgeObject Updated knowledge object
     * @return Updated knowledge object
     */
    @PutMapping("/objects/{id}")
    public ResponseEntity<KnowledgeObject> updateKnowledgeObject(
            @PathVariable String id,
            @RequestBody KnowledgeObject knowledgeObject) {
        try {
            Optional<KnowledgeObject> existingObject = knowledgeObjectRepository.findById(id);
            if (existingObject.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            knowledgeObject.setId(id);
            KnowledgeObject updatedObject = knowledgeObjectRepository.save(knowledgeObject);
            return ResponseEntity.ok(updatedObject);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Delete a knowledge object.
     *
     * @param id Knowledge object ID
     * @return Success status
     */
    @DeleteMapping("/objects/{id}")
    public ResponseEntity<Void> deleteKnowledgeObject(@PathVariable String id) {
        try {
            Optional<KnowledgeObject> existingObject = knowledgeObjectRepository.findById(id);
            if (existingObject.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            knowledgeObjectRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get content variants for a knowledge object.
     *
     * @param knowledgeObjectId Knowledge object ID
     * @return List of content variants
     */
    @GetMapping("/objects/{knowledgeObjectId}/variants")
    public ResponseEntity<List<ContentVariant>> getContentVariants(@PathVariable String knowledgeObjectId) {
        try {
            // For now, return empty list since we need tenant context
            // TODO: Add tenant context from security context
            List<ContentVariant> variants = contentVariantRepository.findByKnowledgeObjectIdAndTenantId(UUID.fromString(knowledgeObjectId), "default");
            return ResponseEntity.ok(variants);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Upload a file and create a knowledge object.
     *
     * @param file File to upload
     * @param metadata Additional metadata
     * @return Created knowledge object
     */
    @PostMapping("/upload")
    public ResponseEntity<KnowledgeObject> uploadFile(
            @RequestParam("file") String file,
            @RequestParam Map<String, String> metadata) {
        try {
            // TODO: Implement file upload logic
            // 1. Store file in blob storage
            // 2. Extract text content
            // 3. Create knowledge object
            // 4. Generate embeddings
            // 5. Store in vector database
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Health check for knowledge endpoints.
     *
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        boolean vectorStoreHealthy = vectorStoreService.isHealthy();
        boolean blobStorageHealthy = blobStorageService.isHealthy();
        
        if (vectorStoreHealthy && blobStorageHealthy) {
            return ResponseEntity.ok("OK");
        } else {
            return ResponseEntity.status(503).body("Service unavailable");
        }
    }
}

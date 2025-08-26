package middleware.api;

import middleware.dto.EmbeddingRequest;
import middleware.dto.EmbeddingResponse;
import middleware.dto.TokenUsage;
import middleware.service.EmbeddingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for embedding generation endpoints.
 * Provides OpenAI-compatible embedding API.
 */
@RestController
@RequestMapping("/v1")
public class EmbeddingsController {

    private final EmbeddingService embeddingService;

    public EmbeddingsController(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    /**
     * Generate embeddings for the given text.
     * OpenAI-compatible endpoint for embedding generation.
     *
     * @param request The embedding request
     * @return The embedding response
     */
    @PostMapping("/embeddings")
    public ResponseEntity<EmbeddingResponse> createEmbedding(@RequestBody EmbeddingRequest request) {
        try {
            // Extract text from request and generate embedding
            String text = request.getInput();
            if (text == null || text.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            List<Float> embedding = embeddingService.generateEmbedding(text);
            
            // Convert List<Float> to List<Double> for the response
            List<Double> embeddingDoubles = embedding.stream()
                .map(Float::doubleValue)
                .toList();
            
            // Create response
            EmbeddingResponse response = EmbeddingResponse.builder()
                .object("list")
                .data(List.of(new EmbeddingResponse.EmbeddingData(embeddingDoubles, 0)))
                .model(embeddingService.getModelName())
                .usage(new TokenUsage(text.length(), 0, text.length()))
                .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // TODO: Add proper error handling and logging
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Health check for the embedding service.
     *
     * @return Health status
     */
    @GetMapping("/embeddings/health")
    public ResponseEntity<String> health() {
        boolean isHealthy = embeddingService.isHealthy();
        if (isHealthy) {
            return ResponseEntity.ok("OK");
        } else {
            return ResponseEntity.status(503).body("Service unavailable");
        }
    }
}

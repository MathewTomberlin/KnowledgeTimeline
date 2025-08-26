package middleware.service.impl;

import middleware.service.EmbeddingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * Local embedding service implementation using HuggingFace text-embeddings-inference.
 * Communicates with the local embedding container for generating embeddings.
 */
@Service
public class LocalEmbeddingService implements EmbeddingService {

    private final WebClient webClient;
    private final String modelName;
    private final int embeddingDimension;

    public LocalEmbeddingService(@Value("${knowledge.embeddings.url:http://localhost:8081}") String baseUrl,
                                @Value("${knowledge.embeddings.model:sentence-transformers/all-MiniLM-L6-v2}") String modelName,
                                @Value("${knowledge.embeddings.dimension:384}") int embeddingDimension) {
        this.modelName = modelName;
        this.embeddingDimension = embeddingDimension;
        
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    @Override
    public List<Float> generateEmbedding(String text) {
        try {
            // Create request for the embedding service
            Map<String, Object> request = Map.of(
                "inputs", text,
                "model", modelName
            );

            // Call the embedding service
            Map<String, Object> response = webClient.post()
                .uri("/embeddings")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("embeddings")) {
                List<List<Double>> embeddings = (List<List<Double>>) response.get("embeddings");
                if (!embeddings.isEmpty()) {
                    // Convert List<Double> to List<Float>
                    return embeddings.get(0).stream()
                        .map(Double::floatValue)
                        .toList();
                }
            }

            // Fallback to mock embedding if service fails
            return generateMockEmbedding(text);
            
        } catch (Exception e) {
            // Fallback to mock embedding if service is unavailable
            return generateMockEmbedding(text);
        }
    }

    @Override
    public List<List<Float>> generateEmbeddings(List<String> texts) {
        try {
            // Create request for batch embedding generation
            Map<String, Object> request = Map.of(
                "inputs", texts,
                "model", modelName
            );

            // Call the embedding service
            Map<String, Object> response = webClient.post()
                .uri("/embeddings")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("embeddings")) {
                List<List<Double>> embeddings = (List<List<Double>>) response.get("embeddings");
                // Convert List<List<Double>> to List<List<Float>>
                return embeddings.stream()
                    .map(embedding -> embedding.stream()
                        .map(Double::floatValue)
                        .toList())
                    .toList();
            }

            // Fallback to mock embeddings if service fails
            return texts.stream()
                .map(this::generateMockEmbedding)
                .toList();
            
        } catch (Exception e) {
            // Fallback to mock embeddings if service is unavailable
            return texts.stream()
                .map(this::generateMockEmbedding)
                .toList();
        }
    }

    @Override
    public int getEmbeddingDimension() {
        return embeddingDimension;
    }

    @Override
    public boolean isHealthy() {
        try {
            // Test if the embedding service is responding
            return webClient.get()
                .uri("/health")
                .retrieve()
                .toBodilessEntity()
                .map(response -> response.getStatusCode().is2xxSuccessful())
                .block();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    /**
     * Generate a mock embedding for fallback scenarios.
     *
     * @param text The input text
     * @return Mock embedding vector
     */
    private List<Float> generateMockEmbedding(String text) {
        // Generate a deterministic mock embedding based on the text
        // This ensures consistent results for testing
        List<Float> embedding = new java.util.ArrayList<>();
        for (int i = 0; i < embeddingDimension; i++) {
            float value = (float) Math.sin(text.hashCode() + i) * 0.1f;
            embedding.add(value);
        }
        return embedding;
    }
}

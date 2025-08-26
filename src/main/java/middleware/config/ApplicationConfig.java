package middleware.config;

import middleware.service.*;
import middleware.service.impl.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * Main application configuration class.
 * Configures service implementations and other application beans.
 */
@Configuration
public class ApplicationConfig {

    /**
     * Configure LLM client service implementation.
     * Uses OpenAI adapter for local development and testing.
     */
    @Bean
    @Profile({"local", "test", "docker"})
    public LLMClientService llmClientService(@Value("${knowledge.llm.api-key:}") String apiKey,
                                            @Value("${knowledge.llm.base-url:https://api.openai.com/v1}") String baseUrl) {
        return new OpenAIAdapter(apiKey, baseUrl);
    }

    /**
     * Configure embedding service implementation.
     * Uses local embedding service for local development and testing.
     */
    @Bean
    @Profile({"local", "test", "docker"})
    public EmbeddingService embeddingService(@Value("${knowledge.embeddings.url:http://localhost:8081}") String baseUrl,
                                           @Value("${knowledge.embeddings.model:sentence-transformers/all-MiniLM-L6-v2}") String modelName,
                                           @Value("${knowledge.embeddings.dimension:384}") int embeddingDimension) {
        return new LocalEmbeddingService(baseUrl, modelName, embeddingDimension);
    }

    /**
     * Configure vector store service implementation.
     * Uses PostgreSQL pgvector adapter for local development and testing.
     */
    @Bean
    @Profile({"local", "test", "docker"})
    public VectorStoreService vectorStoreService(DataSource dataSource,
                                               @Value("${knowledge.embeddings.dimension:384}") int embeddingDimension) {
        return new PostgresPgvectorAdapter(dataSource, embeddingDimension);
    }

    /**
     * Configure context builder service implementation.
     * Uses mock implementation for local development and testing.
     */
    @Bean
    @Profile({"local", "test", "docker"})
    public ContextBuilderService contextBuilderService() {
        return new MockContextBuilderService();
    }

    /**
     * Configure token counting service implementation.
     * Uses mock implementation for local development and testing.
     */
    @Bean
    @Profile({"local", "test", "docker"})
    public TokenCountingService tokenCountingService() {
        return new MockTokenCountingService();
    }

    /**
     * Configure usage tracking service implementation.
     * Uses mock implementation for local development and testing.
     */
    @Bean
    @Profile({"local", "test", "docker"})
    public UsageTrackingService usageTrackingService() {
        return new MockUsageTrackingService();
    }

    /**
     * Configure dialogue state service implementation.
     * Uses mock implementation for local development and testing.
     */
    @Bean
    @Profile({"local", "test", "docker"})
    public DialogueStateService dialogueStateService() {
        return new MockDialogueStateService();
    }

    /**
     * Configure memory extraction service implementation.
     * Uses mock implementation for local development and testing.
     */
    @Bean
    @Profile({"local", "test", "docker"})
    public MemoryExtractionService memoryExtractionService() {
        return new MockMemoryExtractionService();
    }

    /**
     * Configure blob storage service implementation.
     * Uses mock implementation for local development and testing.
     */
    @Bean
    @Profile({"local", "test", "docker"})
    public BlobStorageService blobStorageService() {
        return new MockBlobStorageService();
    }
}

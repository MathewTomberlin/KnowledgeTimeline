package middleware.config;

import middleware.service.*;
import middleware.service.impl.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Main application configuration class.
 * Configures service implementations and other application beans.
 */
@Configuration
public class ApplicationConfig {

    /**
     * Configure LLM client service implementation.
     * Uses mock implementation for local development and testing.
     */
    @Bean
    @Profile({"local", "test", "docker"})
    public LLMClientService llmClientService() {
        return new MockLLMClientService();
    }

    /**
     * Configure embedding service implementation.
     * Uses mock implementation for local development and testing.
     */
    @Bean
    @Profile({"local", "test", "docker"})
    public EmbeddingService embeddingService() {
        return new MockEmbeddingService();
    }

    /**
     * Configure vector store service implementation.
     * Uses mock implementation for local development and testing.
     */
    @Bean
    @Profile({"local", "test", "docker"})
    public VectorStoreService vectorStoreService() {
        return new MockVectorStoreService();
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

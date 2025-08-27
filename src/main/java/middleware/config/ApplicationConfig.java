package middleware.config;

import middleware.service.*;
import middleware.service.impl.*;
import middleware.vector.impl.PostgresPgvectorAdapter;
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
     * Uses OpenAI adapter for local development and docker deployment.
     * Note: Integration tests use MockLLMClientService instead.
     */
    @Bean
    @Profile({"local", "docker"})  // Removed "test" to avoid conflict with MockLLMClientService
    public LLMClientService llmClientService(@Value("${knowledge.llm.api-key:}") String apiKey,
                                            @Value("${knowledge.llm.base-url:https://api.openai.com/v1}") String baseUrl) {
        return new OpenAIAdapter(apiKey, baseUrl);
    }

    /**
     * Configure embedding service implementation.
     * Uses mock embeddings service for development (Ollama handles embeddings in container).
     */
    @Bean
    @Profile({"local", "test", "docker"})
    public EmbeddingService embeddingService() {
        return new MockEmbeddingService();
    }

    /**
     * Configure vector store service implementation.
     * Uses PostgreSQL with pgvector for local development.
     */
    @Bean
    @Profile({"local", "test", "docker"})
    public VectorStoreService vectorStoreService(DataSource dataSource,
                                               @Value("${knowledge.embeddings.dimension:384}") int embeddingDimension) {
        return new PostgresPgvectorAdapter(dataSource);
    }

    /**
     * Configure blob storage service implementation.
     * Uses local disk storage for development.
     */
    @Bean
    @Profile({"local", "test", "docker"})
    public BlobStorageService blobStorageService() {
        return new LocalDiskBlobStorage();
    }

    // TODO: Add production and GCP profile configurations for:
    // - OracleVectorAdapter for production vector storage
    // - OCIObjectStorage for production blob storage
    // - RemoteEmbeddings service for cloud deployments

    // Note: DataSource is handled by Spring Boot's auto-configuration
    // based on application-*.yml profile-specific database settings
}

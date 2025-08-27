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
     * Uses OpenAI adapter for production, Ollama adapter for local/docker development.
     * Note: Integration tests use MockLLMClientService for isolated testing.
     */
    @Bean
    @Profile({"local", "docker", "integration-real"})  // Include integration-real for real LLM testing
    public LLMClientService llmClientService(@Value("${knowledge.llm.base-url:http://localhost:11434}") String baseUrl,
                                            @Value("${knowledge.llm.model:llama2}") String model) {
        return new OllamaAdapter(baseUrl, model);
    }

    /**
     * Configure OpenAI LLM client service implementation for production use.
     */
    @Bean
    @Profile({"production", "gcp"})  // Cloud/production profiles use OpenAI
    public LLMClientService openAILLMClientService(@Value("${knowledge.llm.api-key:}") String apiKey,
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
    public VectorStoreService vectorStoreService(DataSource dataSource, EmbeddingService embeddingService,
                                               @Value("${knowledge.embeddings.dimension:384}") int embeddingDimension) {
        return new PostgresPgvectorAdapter(dataSource, embeddingService, embeddingDimension);
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

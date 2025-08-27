package middleware.integration;

import middleware.KnowledgeMiddlewareApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
// import org.testcontainers.containers.RedisContainer; // Temporarily disabled
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests using Testcontainers.
 * Sets up PostgreSQL and Redis containers for testing.
 */
@SpringBootTest(
    classes = {KnowledgeMiddlewareApplication.class, IntegrationTestConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("integration")
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg15").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("knowledge_middleware_test")
            .withUsername("postgres")
            .withPassword("postgres");

    // Redis container temporarily disabled - will be added back after fixing dependency issues
    // @Container
    // static final RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Wait for container to be ready
        try {
            Thread.sleep(2000); // Give container time to fully start
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Set additional properties that might be needed
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // Redis properties temporarily disabled
        // registry.add("spring.data.redis.host", redis::getHost);
        // registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @BeforeAll
    static void beforeAll() {
        postgres.start();
        // redis.start(); // Temporarily disabled
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
        // redis.stop(); // Temporarily disabled
    }
}

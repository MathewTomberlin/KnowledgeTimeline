package middleware.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;
// import org.testcontainers.containers.RedisContainer; // Temporarily disabled
import org.testcontainers.utility.DockerImageName;

/**
 * Test configuration for integration tests using Testcontainers.
 * Provides containerized PostgreSQL and Redis instances for testing.
 */
@TestConfiguration
@Profile("integration")
public class IntegrationTestConfig {

    // PostgreSQL container is managed by BaseIntegrationTest.java using @Container
    // This class focuses on providing test-specific beans like PasswordEncoder

    // Redis container temporarily disabled - will be added back after fixing dependency issues
    // @Bean
    // public RedisContainer redisContainer() {
    //     RedisContainer container = new RedisContainer(DockerImageName.parse("redis:7-alpine"));
    //
    //     container.start();
    //
    //     // Set system properties for Spring to use
    //     System.setProperty("spring.data.redis.host", container.getHost());
    //     System.setProperty("spring.data.redis.port", String.valueOf(container.getFirstMappedPort()));
    //
    //     return container;
    // }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // TestRestTemplate will be auto-configured by Spring Boot when using
    // @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

}

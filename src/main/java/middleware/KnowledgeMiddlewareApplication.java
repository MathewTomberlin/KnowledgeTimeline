package middleware;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application for Knowledge-Aware LLM Middleware.
 * 
 * Features:
 * - OpenAI-compatible API endpoints
 * - Multi-tenant architecture
 * - Vector similarity search
 * - Context building with token budgets
 * - Memory extraction and relationship discovery
 * - Usage tracking and billing
 * - Rate limiting and caching
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class KnowledgeMiddlewareApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeMiddlewareApplication.class, args);
    }
}

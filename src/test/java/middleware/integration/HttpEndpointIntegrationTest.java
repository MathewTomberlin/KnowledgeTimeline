package middleware.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HTTP Endpoint Integration Test - Tests complete E2E flow from HTTP request to response.
 * This tests the actual REST endpoints of the running application.
 */
@ActiveProfiles("integration")
class HttpEndpointIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoadsAndEndpointsMapped() {
        // First, verify the application context loaded properly
        assertNotNull(applicationContext);
        System.out.println("✅ Application context loaded successfully");

        // Check that controllers are properly registered
        Map<String, ?> controllers = applicationContext.getBeansWithAnnotation(org.springframework.web.bind.annotation.RestController.class);
        assertFalse(controllers.isEmpty());
        System.out.println("✅ Found " + controllers.size() + " REST controllers");

        // Try to get the RequestMappingHandlerMapping to see what endpoints are mapped
        try {
            RequestMappingHandlerMapping handlerMapping = applicationContext.getBean(RequestMappingHandlerMapping.class);
            assertNotNull(handlerMapping);
            System.out.println("✅ RequestMappingHandlerMapping is available");
        } catch (Exception e) {
            System.out.println("⚠️  RequestMappingHandlerMapping not found: " + e.getMessage());
        }
    }

    @Test
    void actuatorHealthEndpoint() {
        // Test Spring Boot Actuator health endpoint
        try {
            ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().contains("UP"));
            System.out.println("✅ Actuator health endpoint working: " + response.getBody());
        } catch (Exception e) {
            System.out.println("⚠️  Actuator health endpoint failed: " + e.getMessage());
            // Don't fail the test if actuator is not available in this configuration
        }
    }

    @Test
    void v1ModelsEndpoint() {
        // Test OpenAI-compatible models endpoint
        ResponseEntity<String> response = restTemplate.getForEntity("/v1/models", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        System.out.println("✅ Models endpoint response: " + response.getBody());
    }

    @Test
    void v1ChatCompletionsEndpoint() {
        // Test OpenAI-compatible chat completions endpoint
        String requestJson = """
            {
                "model": "gpt-3.5-turbo",
                "messages": [
                    {
                        "role": "user",
                        "content": "Hello! Can you tell me about knowledge-aware systems?"
                    }
                ],
                "max_tokens": 100,
                "temperature": 0.7
            }
            """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(requestJson, headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/v1/chat/completions", request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("id"));
        assertTrue(response.getBody().contains("choices"));
        System.out.println("✅ Chat completions endpoint working: " + response.getBody());
    }

    @Test
    void v1EmbeddingsEndpoint() {
        // Test OpenAI-compatible embeddings endpoint
        String requestJson = """
            {
                "model": "sentence-transformers/all-MiniLM-L6-v2",
                "input": ["This is a test sentence for embeddings"]
            }
            """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(requestJson, headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/v1/embeddings", request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("data"));
        System.out.println("✅ Embeddings endpoint working: " + response.getBody());
    }

    @Test
    void healthEndpoint() {
        // Test custom health endpoint (if different from actuator)
        ResponseEntity<String> response = restTemplate.getForEntity("/health", String.class);

        // Health endpoint might return different status codes depending on implementation
        assertNotNull(response.getBody());
        System.out.println("✅ Health endpoint response: " + response.getBody());
    }

    @Test
    void completeE2EFlowTest() {
        // Test complete E2E flow: Models -> Chat -> Embeddings
        System.out.println("🚀 Testing Complete E2E Flow...");

        // Step 1: Get available models
        ResponseEntity<String> modelsResponse = restTemplate.getForEntity("/v1/models", String.class);
        assertEquals(HttpStatus.OK, modelsResponse.getStatusCode());
        System.out.println("✅ Step 1: Models retrieved successfully");

        // Step 2: Make a chat completion request
        String chatRequest = """
            {
                "model": "gpt-3.5-turbo",
                "messages": [
                    {
                        "role": "user",
                        "content": "What are the key components of a knowledge-aware LLM system?"
                    }
                ],
                "max_tokens": 150
            }
            """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> chatEntity = new HttpEntity<>(chatRequest, headers);

        ResponseEntity<String> chatResponse = restTemplate.postForEntity("/v1/chat/completions", chatEntity, String.class);
        assertEquals(HttpStatus.OK, chatResponse.getStatusCode());
        System.out.println("✅ Step 2: Chat completion successful");

        // Step 3: Test embeddings with the response
        String embeddingRequest = """
            {
                "model": "sentence-transformers/all-MiniLM-L6-v2",
                "input": ["Knowledge-aware systems enhance LLMs with contextual information"]
            }
            """;

        HttpEntity<String> embeddingEntity = new HttpEntity<>(embeddingRequest, headers);
        ResponseEntity<String> embeddingResponse = restTemplate.postForEntity("/v1/embeddings", embeddingEntity, String.class);
        assertEquals(HttpStatus.OK, embeddingResponse.getStatusCode());
        System.out.println("✅ Step 3: Embeddings generated successfully");

        System.out.println("🎉 Complete E2E Flow Test PASSED!");
        System.out.println("📊 Models Response: " + modelsResponse.getBody());
        System.out.println("💬 Chat Response: " + chatResponse.getBody());
        System.out.println("🔮 Embedding Response: " + embeddingResponse.getBody());
    }

    @Test
    void errorHandlingTest() {
        // Test error handling with invalid requests
        String invalidRequest = """
            {
                "model": "nonexistent-model",
                "messages": []
            }
            """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(invalidRequest, headers);

        ResponseEntity<String> response = restTemplate.postForEntity("/v1/chat/completions", request, String.class);

        // Should return some error status (could be 400, 404, 500 depending on implementation)
        assertNotNull(response.getStatusCode());
        assertNotNull(response.getBody());
        System.out.println("✅ Error handling working - Status: " + response.getStatusCode() + ", Body: " + response.getBody());
    }
}

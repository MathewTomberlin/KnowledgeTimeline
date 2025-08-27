package middleware.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Docker Compose integration test.
 * Tests the complete application running in containerized environment.
 */
class DockerComposeIntegrationTest {

    private static final String APPLICATION_URL = "http://localhost:8080";
    private static Process dockerComposeProcess;
    private RestTemplate restTemplate = new RestTemplate();
    private MockMvc mockMvc;

    @BeforeAll
    static void startDockerCompose() throws Exception {
        System.out.println("Starting Docker Compose environment...");

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("docker-compose", "up", "-d")
                     .directory(new java.io.File("."))
                     .inheritIO();

        dockerComposeProcess = processBuilder.start();
        dockerComposeProcess.waitFor(); // Wait for docker-compose to finish starting

        // Wait for services to be ready
        System.out.println("Waiting for services to be ready...");
        Thread.sleep(30000); // Give services time to start

        // Check if services are healthy
        waitForService("http://localhost:8080/actuator/health", 60);
        System.out.println("Docker Compose environment is ready!");
    }

    @AfterAll
    static void stopDockerCompose() throws Exception {
        System.out.println("Stopping Docker Compose environment...");
        if (dockerComposeProcess != null) {
            ProcessBuilder stopProcess = new ProcessBuilder();
            stopProcess.command("docker-compose", "down")
                      .directory(new java.io.File("."))
                      .inheritIO();
            stopProcess.start().waitFor();
        }
    }

    private static void waitForService(String healthUrl, int maxRetries) {
        RestTemplate template = new RestTemplate();
        for (int i = 0; i < maxRetries; i++) {
            try {
                String response = template.getForObject(healthUrl, String.class);
                if (response != null && response.contains("UP")) {
                    System.out.println("Service is healthy: " + healthUrl);
                    return;
                }
            } catch (Exception e) {
                System.out.println("Waiting for service... attempt " + (i + 1) + "/" + maxRetries);
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        throw new RuntimeException("Service did not become healthy: " + healthUrl);
    }

    @Test
    void containerizedApplicationHealth() {
        // Test that the containerized application is healthy
        String response = restTemplate.getForObject(APPLICATION_URL + "/actuator/health", String.class);
        assert response != null;
        assert response.contains("UP");
        System.out.println("Containerized application health check passed!");
    }

    @Test
    void containerizedApplicationModels() {
        // Test that the containerized application can serve models
        String response = restTemplate.getForObject(APPLICATION_URL + "/v1/models", String.class);
        assert response != null;
        System.out.println("Models response: " + response);
    }

    @Test
    void containerizedApplicationChatCompletion() {
        // Test chat completion through containerized application
        String requestJson = """
            {
                "model": "llama2",
                "messages": [{"role": "user", "content": "Hello from container test!"}],
                "max_tokens": 50
            }
            """;

        String response = restTemplate.postForObject(
            APPLICATION_URL + "/v1/chat/completions",
            requestJson,
            String.class
        );

        assert response != null;
        assert response.contains("id");
        assert response.contains("choices");
        System.out.println("Chat completion response: " + response);
    }

    @Test
    void containerizedApplicationEmbeddings() {
        // Test embeddings through containerized application
        String requestJson = """
            {
                "model": "sentence-transformers/all-MiniLM-L6-v2",
                "input": ["Container test embedding"]
            }
            """;

        String response = restTemplate.postForObject(
            APPLICATION_URL + "/v1/embeddings",
            requestJson,
            String.class
        );

        assert response != null;
        assert response.contains("data");
        System.out.println("Embeddings response: " + response);
    }

    @Test
    void containerNetworking() {
        // Test that containers can communicate with each other
        // This verifies that the Docker network is properly configured
        String healthResponse = restTemplate.getForObject(APPLICATION_URL + "/actuator/health", String.class);
        assert healthResponse.contains("UP");

        // If we get here, it means:
        // 1. The middleware container is running
        // 2. It can connect to PostgreSQL
        // 3. It can connect to Ollama (if enabled)
        // 4. The Docker network is working
        System.out.println("Container networking test passed!");
    }
}

package middleware.integration;

import middleware.dto.ChatCompletionRequest;
import middleware.dto.ChatCompletionResponse;
import middleware.dto.ChatMessage;
import middleware.model.KnowledgeObject;
import middleware.model.KnowledgeObjectType;
import middleware.repository.KnowledgeObjectRepository;
import middleware.service.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test using real Ollama service for complete E2E testing.
 * This tests the actual LLM functionality and data persistence.
 */
@ActiveProfiles("integration-real")
class RealServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private LLMClientService llmClientService;

    @Autowired
    private ContextBuilderService contextBuilderService;

    @Autowired
    private MemoryExtractionService memoryExtractionService;

    @Autowired
    private KnowledgeObjectRepository knowledgeObjectRepository;

    @Test
    void ollamaHealthCheck() {
        // Test that Ollama service is healthy
        assertTrue(llmClientService.isHealthy(), "Ollama service should be healthy");
    }

    @Test
    void realLLMChatCompletion() {
        // Test actual LLM call with Ollama
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("llama2")
                .messages(List.of(new ChatMessage("user", "Say hello in exactly 3 words.")))
                .maxTokens(50)
                .build();

        ChatCompletionResponse response = llmClientService.createChatCompletion(request);

        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getId(), "Response should have an ID");
        assertNotNull(response.getChoices(), "Response should have choices");
        assertFalse(response.getChoices().isEmpty(), "Response should have at least one choice");

        String content = response.getChoices().get(0).getMessage().getContent();
        assertNotNull(content, "Response should have content");
        assertFalse(content.trim().isEmpty(), "Content should not be empty");

        System.out.println("LLM Response: " + content);
    }

    @Test
    void realMemoryExtraction() {
        // Test memory extraction with real LLM
        String userMessage = "I am learning Java Spring Boot and really enjoying it.";
        String assistantMessage = "That's wonderful! Spring Boot makes Java development much easier.";

        MemoryExtractionService.MemoryExtraction result = memoryExtractionService.extractMemory(
                userMessage, assistantMessage, java.util.Map.of());

        assertNotNull(result, "Memory extraction result should not be null");
        System.out.println("Extracted Facts: " + result.getFacts());
        System.out.println("Extracted Entities: " + result.getEntities());
        System.out.println("Extracted Tasks: " + result.getTasks());
    }

    @Test
    void realContextBuilding() {
        // Test context building with real vector search
        String query = "What programming frameworks should I learn?";

        String context = contextBuilderService.buildContext(
                "test-tenant", "test-session", query, java.util.Map.of());

        assertNotNull(context, "Context should not be null");
        System.out.println("Built Context: " + context);
    }

    @Test
    void completeE2EFlow() {
        // Test complete flow: LLM call -> Memory extraction -> Persistence
        String tenantId = "e2e-test-tenant";
        String sessionId = "e2e-test-session";
        String userId = "e2e-test-user";

        // 1. Make LLM call
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("llama2")
                .messages(List.of(new ChatMessage("user", "What is the capital of France?")))
                .maxTokens(100)
                .build();

        ChatCompletionResponse response = llmClientService.createChatCompletion(request);
        String assistantResponse = response.getChoices().get(0).getMessage().getContent();

        // 2. Extract memory
        MemoryExtractionService.MemoryExtraction memory = memoryExtractionService.extractMemory(
                "What is the capital of France?", assistantResponse, java.util.Map.of());

        // 3. Persist knowledge objects
        KnowledgeObject userMessageObj = new KnowledgeObject();
        userMessageObj.setTenantId(tenantId);
        userMessageObj.setSessionId(sessionId);
        userMessageObj.setUserId(userId);
        userMessageObj.setType(KnowledgeObjectType.TURN);
        userMessageObj.setArchived(false);
        knowledgeObjectRepository.save(userMessageObj);

        KnowledgeObject assistantMessageObj = new KnowledgeObject();
        assistantMessageObj.setTenantId(tenantId);
        assistantMessageObj.setSessionId(sessionId);
        assistantMessageObj.setUserId(userId);
        assistantMessageObj.setType(KnowledgeObjectType.TURN);
        assistantMessageObj.setArchived(false);
        knowledgeObjectRepository.save(assistantMessageObj);

        // 4. Verify persistence
        List<KnowledgeObject> savedObjects = knowledgeObjectRepository
                .findAll()
                .stream()
                .filter(obj -> tenantId.equals(obj.getTenantId()))
                .toList();

        assertFalse(savedObjects.isEmpty(), "Knowledge objects should be persisted");

        System.out.println("E2E Flow completed successfully!");
        System.out.println("LLM Response: " + assistantResponse);
        System.out.println("Extracted Memory: " + memory.getFacts());
        System.out.println("Persisted Objects: " + savedObjects.size());
    }

    @Test
    void availableModels() {
        // Test that we can get available models from Ollama
        List<middleware.dto.Model> models = llmClientService.getAvailableModels();

        assertNotNull(models, "Models list should not be null");
        assertFalse(models.isEmpty(), "Models list should not be empty");

        System.out.println("Available models: " + models.stream()
                .map(middleware.dto.Model::getId)
                .toList());
    }
}

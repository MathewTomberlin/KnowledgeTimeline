package middleware.integration;

import middleware.dto.ChatCompletionRequest;
import middleware.dto.ChatCompletionResponse;
import middleware.dto.ChatMessage;
import middleware.model.KnowledgeObject;
import middleware.model.KnowledgeObjectType;
import middleware.repository.KnowledgeObjectRepository;
import middleware.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;

/**
 * Comprehensive integration test for the Knowledge-Aware LLM Middleware.
 * Tests the complete application with containerized services and live data.
 */
class ApplicationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private KnowledgeObjectRepository knowledgeObjectRepository;

    @Autowired
    private LLMClientService llmClientService;

    @Autowired
    private ContextBuilderService contextBuilderService;

    @Autowired
    private MemoryExtractionService memoryExtractionService;

    @Test
    void contextLoads() {
        // Test that the application context loads successfully with containers
        assert applicationContext != null;
        assert applicationContext.getBeanDefinitionCount() > 0;

        // Verify all critical services are available
        assert applicationContext.getBean("dataSource") != null;
        assert llmClientService != null;
        assert contextBuilderService != null;
        assert memoryExtractionService != null;
    }

    @Test
    void databaseConnectionWorks() {
        // Verify that we can access the database through the context
        assert applicationContext.getBean("dataSource") != null;

        // Test basic database operations
        List<KnowledgeObject> objects = knowledgeObjectRepository.findAll();
        assert objects != null; // Database connection works
    }

    @Test
    void llmServiceIntegration() {
        // Test LLM service integration with mock data
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(List.of(new ChatMessage("user", "Hello, how are you?")))
                .maxTokens(100)
                .build();

        ChatCompletionResponse response = llmClientService.createChatCompletion(request);

        assert response != null;
        assert response.getId() != null;
        assert response.getModel() != null;
        assert response.getChoices() != null && !response.getChoices().isEmpty();
    }

    @Test
    void memoryExtractionIntegration() {
        // Test memory extraction with sample conversation
        String userMessage = "I love programming with Java and Spring Boot.";
        String assistantMessage = "That's great! Java and Spring Boot are excellent choices for enterprise applications.";

        Map<String, Object> context = Map.of("sessionId", "test-session", "userId", "test-user");

        MemoryExtractionService.MemoryExtraction result = memoryExtractionService.extractMemory(
                userMessage, assistantMessage, context);

        assert result != null;
        assert result.getFacts() != null;
        assert result.getEntities() != null;
        assert result.getTasks() != null;
    }

    @Test
    void contextBuildingIntegration() {
        // Test context building with vector search
        String tenantId = "test-tenant";
        String sessionId = "test-session";
        String userPrompt = "What programming languages do you recommend?";

        String context = contextBuilderService.buildContext(tenantId, sessionId, userPrompt, Map.of());

        assert context != null;
        assert !context.isEmpty();
    }

    @Test
    void knowledgeObjectPersistence() {
        // Test creating and persisting knowledge objects
        KnowledgeObject knowledgeObject = new KnowledgeObject();
        knowledgeObject.setTenantId("test-tenant");
        knowledgeObject.setSessionId("test-session");
        knowledgeObject.setUserId("test-user");
        knowledgeObject.setType(KnowledgeObjectType.TURN);
        knowledgeObject.setArchived(false);

        KnowledgeObject saved = knowledgeObjectRepository.save(knowledgeObject);
        assert saved.getId() != null;

        // Verify retrieval
        List<KnowledgeObject> retrieved = knowledgeObjectRepository.findAll();
        assert !retrieved.isEmpty();
    }

    @Test
    void healthEndpoint() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/actuator/health"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status").value("UP"));
    }

    @Test
    void modelsEndpoint() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/v1/models"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$").isArray());
    }

    @Test
    void chatCompletionEndpoint() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        String requestJson = """
            {
                "model": "llama2",
                "messages": [{"role": "user", "content": "Hello!"}],
                "max_tokens": 100
            }
            """;

        mockMvc.perform(post("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.id").exists())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.choices[0].message.content").exists());
    }

    @Test
    void embeddingsEndpoint() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        String requestJson = """
            {
                "model": "sentence-transformers/all-MiniLM-L6-v2",
                "input": ["Hello world"]
            }
            """;

        mockMvc.perform(post("/v1/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data").isArray());
    }

    // TODO: Add container deployment tests
    // @Test
    // void containerizedApplicationStarts() {
    //     // Test that the application starts correctly in Docker container
    //     // This would require Docker Compose integration testing
    // }
}

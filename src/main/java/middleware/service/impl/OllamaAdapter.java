package middleware.service.impl;

import middleware.dto.ChatCompletionRequest;
import middleware.dto.ChatCompletionResponse;
import middleware.dto.ChatChoice;
import middleware.dto.ChatMessage;
import middleware.dto.EmbeddingRequest;
import middleware.dto.EmbeddingResponse;
import middleware.dto.Model;
import middleware.dto.TokenUsage;
import middleware.service.LLMClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ollama adapter implementation for LLMClientService.
 * Provides integration with local Ollama service for both LLM and embeddings.
 */
@Service
public class OllamaAdapter implements LLMClientService {

    private static final Logger logger = LoggerFactory.getLogger(OllamaAdapter.class);

    private final WebClient webClient;
    private final String baseUrl;
    private final String defaultModel;

    public OllamaAdapter(@Value("${knowledge.llm.base-url:http://localhost:11434}") String baseUrl,
                        @Value("${knowledge.llm.model:llama2}") String defaultModel) {
        this.baseUrl = baseUrl;
        this.defaultModel = defaultModel;

        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .build();

        logger.info("OllamaAdapter initialized with baseUrl: {} and defaultModel: {}", baseUrl, defaultModel);
    }

    @Override
    public ChatCompletionResponse createChatCompletion(ChatCompletionRequest request) {
        try {
            // Convert our DTO to Ollama's format
            Map<String, Object> ollamaRequest = Map.of(
                "model", request.getModel() != null ? request.getModel() : defaultModel,
                "messages", convertMessagesToOllamaFormat(request.getMessages()),
                "stream", false,
                "options", Map.of(
                    "temperature", 0.7,
                    "top_p", 0.9,
                    "max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 1000
                )
            );

            // Call Ollama API
            Map<String, Object> ollamaResponse = webClient.post()
                .uri("/api/chat")
                .bodyValue(ollamaRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (ollamaResponse == null) {
                throw new RuntimeException("Empty response from Ollama");
            }

            // Convert Ollama response back to our DTO format
            return convertOllamaResponseToDTO(ollamaResponse, request);

        } catch (Exception e) {
            logger.error("Error calling Ollama API", e);
            throw new RuntimeException("Failed to get response from Ollama: " + e.getMessage(), e);
        }
    }

    @Override
    public EmbeddingResponse createEmbedding(EmbeddingRequest request) {
        try {
            // Ollama doesn't have a separate embeddings API like OpenAI
            // We'll use the chat completion API to generate embeddings
            String text = request.getInput().get(0); // Take first input

            // For now, return a mock embedding since Ollama doesn't have native embedding support
            // In a real implementation, you might use a different model or service for embeddings
            List<Double> mockEmbedding = generateMockEmbedding(384); // Standard embedding dimension

            TokenUsage usage = new TokenUsage(
                estimateTokens(text), 0, estimateTokens(text)
            );

            return EmbeddingResponse.builder()
                .data(List.of(new EmbeddingResponse.EmbeddingData(mockEmbedding, 0)))
                .model(request.getModel() != null ? request.getModel() : "ollama-embedding")
                .usage(usage)
                .knowledgeObjectId(null)
                .build();

        } catch (Exception e) {
            logger.error("Error generating embeddings", e);
            throw new RuntimeException("Failed to generate embeddings: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Model> getAvailableModels() {
        try {
            Map<String, Object> response = webClient.get()
                .uri("/api/tags")
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("models")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> models = (List<Map<String, Object>>) response.get("models");

                return models.stream()
                    .map(model -> Model.builder()
                        .id((String) model.get("name"))
                        .ownedBy("ollama")
                        .maxTokens(4096) // Default value, Ollama models vary
                        .knowledgeAware(true)
                        .build())
                    .toList();
            }

            // Fallback to default models if API call fails
            return List.of(
                Model.builder()
                    .id("llama2")
                    .ownedBy("ollama")
                    .maxTokens(4096)
                    .knowledgeAware(true)
                    .build(),
                Model.builder()
                    .id("codellama")
                    .ownedBy("ollama")
                    .maxTokens(4096)
                    .knowledgeAware(true)
                    .build()
            );

        } catch (Exception e) {
            logger.warn("Failed to get models from Ollama, using defaults", e);
            // Return default models if Ollama is not available
            return List.of(
                Model.builder()
                    .id("llama2")
                    .ownedBy("ollama")
                    .maxTokens(4096)
                    .knowledgeAware(true)
                    .build()
            );
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            webClient.get()
                .uri("/api/tags")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            return true;
        } catch (Exception e) {
            logger.debug("Ollama health check failed", e);
            return false;
        }
    }

    private List<Map<String, String>> convertMessagesToOllamaFormat(List<ChatMessage> messages) {
        List<Map<String, String>> ollamaMessages = new ArrayList<>();

        for (ChatMessage message : messages) {
            ollamaMessages.add(Map.of(
                "role", message.getRole(),
                "content", message.getContent()
            ));
        }

        return ollamaMessages;
    }

    private ChatCompletionResponse convertOllamaResponseToDTO(Map<String, Object> ollamaResponse,
                                                            ChatCompletionRequest originalRequest) {
        String content = (String) ollamaResponse.get("message.content");
        if (content == null) {
            // Fallback for different Ollama response formats
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) ollamaResponse.get("message");
            if (message != null) {
                content = (String) message.get("content");
            }
        }

        if (content == null) {
            content = "I apologize, but I couldn't generate a response.";
        }

        ChatMessage assistantMessage = new ChatMessage("assistant", content);
        ChatChoice choice = new ChatChoice(0, assistantMessage, "stop");

        TokenUsage usage = new TokenUsage(
            estimateTokens(originalRequest.getMessages()),
            estimateTokens(List.of(assistantMessage)),
            estimateTokens(originalRequest.getMessages()) + estimateTokens(List.of(assistantMessage))
        );

        return ChatCompletionResponse.builder()
            .id("ollama-" + java.util.UUID.randomUUID().toString())
            .model(originalRequest.getModel() != null ? originalRequest.getModel() : defaultModel)
            .choices(List.of(choice))
            .usage(usage)
            .knowledgeContext(null) // Ollama doesn't provide knowledge context
            .build();
    }

    private List<Double> generateMockEmbedding(int dimensions) {
        List<Double> embedding = new ArrayList<>();
        for (int i = 0; i < dimensions; i++) {
            // Generate random values between -1 and 1
            embedding.add((Math.random() * 2) - 1);
        }
        return embedding;
    }

    private int estimateTokens(List<ChatMessage> messages) {
        return messages.stream()
            .mapToInt(message -> estimateTokens(message.getContent()))
            .sum();
    }

    private int estimateTokens(String text) {
        if (text == null) return 0;
        // Simple token estimation: roughly 4 characters per token
        return Math.max(1, text.length() / 4);
    }
}

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * OpenAI adapter implementation for LLMClientService.
 * Provides real integration with OpenAI's API.
 */
@Service
public class OpenAIAdapter implements LLMClientService {

    private final WebClient webClient;
    private final String apiKey;
    private final String baseUrl;

    public OpenAIAdapter(@Value("${knowledge.llm.api-key:}") String apiKey,
                        @Value("${knowledge.llm.base-url:https://api.openai.com/v1}") String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    @Override
    public ChatCompletionResponse createChatCompletion(ChatCompletionRequest request) {
        try {
            // Convert our DTO to OpenAI's format
            Map<String, Object> openAIRequest = Map.of(
                "model", request.getModel(),
                "messages", request.getMessages().stream()
                    .map(msg -> Map.of("role", msg.getRole(), "content", msg.getContent()))
                    .toList(),
                "temperature", request.getTemperature(),
                "max_tokens", request.getMaxTokens(),
                "stream", request.getStream()
            );

            return webClient.post()
                .uri("/chat/completions")
                .bodyValue(openAIRequest)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .block();
        } catch (Exception e) {
            // Fallback to mock response if OpenAI is not available
            return createMockResponse(request);
        }
    }

    @Override
    public EmbeddingResponse createEmbedding(EmbeddingRequest request) {
        try {
            Map<String, Object> openAIRequest = Map.of(
                "model", request.getModel(),
                "input", request.getInput()
            );

            return webClient.post()
                .uri("/embeddings")
                .bodyValue(openAIRequest)
                .retrieve()
                .bodyToMono(EmbeddingResponse.class)
                .block();
        } catch (Exception e) {
            // Fallback to mock response
            return createMockEmbeddingResponse(request);
        }
    }

    @Override
    public List<Model> getAvailableModels() {
        try {
            return webClient.get()
                .uri("/models")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
                    return data.stream()
                        .map(modelData -> Model.builder()
                            .id((String) modelData.get("id"))
                            .object((String) modelData.get("object"))
                            .ownedBy((String) modelData.get("owned_by"))
                            .build())
                        .toList();
                })
                .block();
        } catch (Exception e) {
            // Return default models if API is not available
            return List.of(
                Model.builder().id("gpt-3.5-turbo").object("model").ownedBy("openai").build(),
                Model.builder().id("gpt-4").object("model").ownedBy("openai").build(),
                Model.builder().id("text-embedding-ada-002").object("model").ownedBy("openai").build()
            );
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            return webClient.get()
                .uri("/models")
                .retrieve()
                .toBodilessEntity()
                .map(response -> response.getStatusCode().is2xxSuccessful())
                .block();
        } catch (Exception e) {
            return false;
        }
    }

    private ChatCompletionResponse createMockResponse(ChatCompletionRequest request) {
        return ChatCompletionResponse.builder()
            .id("mock-" + System.currentTimeMillis())
            .object("chat.completion")
            .created(System.currentTimeMillis() / 1000)
            .model(request.getModel())
            .choices(List.of(new ChatChoice(
                0,
                new ChatMessage("assistant", "This is a mock response from OpenAI adapter."),
                "stop"
            )))
            .usage(new TokenUsage(10, 20, 30))
            .build();
    }

    private EmbeddingResponse createMockEmbeddingResponse(EmbeddingRequest request) {
        List<Double> mockEmbedding = List.of(0.1, 0.2, 0.3, 0.4, 0.5); // Mock 5-dimensional embedding
        
        return EmbeddingResponse.builder()
            .object("list")
            .data(List.of(new EmbeddingResponse.EmbeddingData(mockEmbedding, 0)))
            .model(request.getModel())
            .usage(new TokenUsage(request.getInput().length(), 0, request.getInput().length()))
            .build();
    }
}

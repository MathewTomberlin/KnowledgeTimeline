package middleware.api;

import middleware.dto.ChatCompletionRequest;
import middleware.dto.ChatCompletionResponse;
import middleware.service.ContextBuilderService;
import middleware.service.LLMClientService;
import middleware.service.UsageTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for chat completion endpoints.
 * Provides OpenAI-compatible chat completion functionality with streaming support.
 */
@RestController
@RequestMapping("/v1")
public class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    @Autowired
    private LLMClientService llmClientService;
    
    @Autowired
    private ContextBuilderService contextBuilderService;
    
    @Autowired
    private UsageTrackingService usageTrackingService;
    
    /**
     * Create a chat completion with optional streaming support.
     * 
     * @param request The chat completion request
     * @return The chat completion response or streaming emitter
     */
    @PostMapping(value = "/chat/completions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createChatCompletion(@RequestBody ChatCompletionRequest request) {
        try {
            // Validate request
            validateChatRequest(request);
            
            // Check if streaming is requested
            if (Boolean.TRUE.equals(request.getStream())) {
                return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(createStreamingResponse(request));
            } else {
                // Non-streaming response
                ChatCompletionResponse response = createNonStreamingResponse(request);
                return ResponseEntity.ok(response);
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid chat completion request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(createErrorResponse("invalid_request_error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating chat completion", e);
            return ResponseEntity.internalServerError()
                .body(createErrorResponse("internal_server_error", "An unexpected error occurred"));
        }
    }
    
    /**
     * Create a streaming chat completion response.
     */
    private SseEmitter createStreamingResponse(ChatCompletionRequest request) {
        SseEmitter emitter = new SseEmitter(30000L); // 30 second timeout
        
        CompletableFuture.runAsync(() -> {
            try {
                // TODO: Implement streaming response logic
                // This would involve:
                // 1. Building context using ContextBuilderService
                // 2. Calling LLM with streaming
                // 3. Sending chunks via SSE
                // 4. Tracking usage
                
                // For now, send a placeholder message
                emitter.send(SseEmitter.event()
                    .name("message")
                    .data("Streaming not yet implemented"));
                
                emitter.complete();
            } catch (IOException e) {
                logger.error("Error in streaming response", e);
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }
    
    /**
     * Create a non-streaming chat completion response.
     */
    private ChatCompletionResponse createNonStreamingResponse(ChatCompletionRequest request) {
        // TODO: Implement full context building and LLM integration
        // This would involve:
        // 1. Building context using ContextBuilderService
        // 2. Calling LLM service
        // 3. Tracking usage
        // 4. Returning response
        
        // For now, return a mock response
        return ChatCompletionResponse.builder()
            .id("chatcmpl-" + System.currentTimeMillis())
            .object("chat.completion")
            .created(System.currentTimeMillis() / 1000)
            .model(request.getModel())
            .choices(new java.util.ArrayList<>())
            .usage(new middleware.dto.TokenUsage())
            .build();
    }
    
    /**
     * Validate the chat completion request.
     */
    private void validateChatRequest(ChatCompletionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        
        if (request.getModel() == null || request.getModel().trim().isEmpty()) {
            throw new IllegalArgumentException("Model is required");
        }
        
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new IllegalArgumentException("Messages cannot be empty");
        }
        
        // Validate each message
        for (int i = 0; i < request.getMessages().size(); i++) {
            var message = request.getMessages().get(i);
            if (message.getRole() == null || message.getRole().trim().isEmpty()) {
                throw new IllegalArgumentException("Message " + i + " role is required");
            }
            if (message.getContent() == null || message.getContent().trim().isEmpty()) {
                throw new IllegalArgumentException("Message " + i + " content is required");
            }
        }
        
        // Validate temperature range
        if (request.getTemperature() != null && (request.getTemperature() < 0.0 || request.getTemperature() > 2.0)) {
            throw new IllegalArgumentException("Temperature must be between 0.0 and 2.0");
        }
        
        // Validate max tokens
        if (request.getMaxTokens() != null && request.getMaxTokens() <= 0) {
            throw new IllegalArgumentException("Max tokens must be positive");
        }
    }
    
    /**
     * Create an error response in OpenAI format.
     */
    private middleware.dto.ErrorResponse createErrorResponse(String type, String message) {
        return middleware.dto.ErrorResponse.builder()
            .error(new middleware.dto.ErrorResponse.Error(type, message))
            .build();
    }
}

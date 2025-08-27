package middleware.api;

import middleware.dto.*;
import middleware.service.ContextBuilderService;
import middleware.service.LLMClientService;
import middleware.service.UsageTrackingService;
import middleware.service.MemoryStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
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
    
    @Autowired
    private MemoryStorageService memoryStorageService;
    
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
                // Extract tenant and session from request context
                String tenantId = extractTenantId();
                String sessionId = extractSessionId(request);
                
                // Build context using ContextBuilderService
                String context = contextBuilderService.buildContext(
                    tenantId, 
                    sessionId, 
                    getLastUserMessage(request), 
                    null
                );
                
                // Create enhanced request with context
                ChatCompletionRequest enhancedRequest = enhanceRequestWithContext(request, context);
                
                // Call LLM service with streaming
                // Note: For now, we'll simulate streaming since the current OpenAIAdapter doesn't support it
                // In a real implementation, this would use a streaming-capable LLM client
                
                // Send context information
                emitter.send(SseEmitter.event()
                    .name("context")
                    .data("Context built: " + context.substring(0, Math.min(100, context.length())) + "..."));
                
                // Send streaming response chunks
                String response = "This is a simulated streaming response. In production, this would be real-time LLM output.";
                String[] chunks = response.split(" ");
                
                for (String chunk : chunks) {
                    emitter.send(SseEmitter.event()
                        .name("chunk")
                        .data(chunk + " "));
                    
                    // Simulate processing delay
                    Thread.sleep(100);
                }
                
                // Store conversation and extract memories after streaming completes
                try {
                    String userMessage = getLastUserMessage(request);
                    Map<String, Object> contextMetadata = Map.of("context_length", context.length(), "model", request.getModel());

                    logger.info("About to call memory storage service for tenant: {}, session: {}", tenantId, sessionId);
                    memoryStorageService.processConversationTurn(
                        tenantId, sessionId, "default-user", userMessage, response, contextMetadata);

                    logger.info("Successfully processed conversation turn for tenant: {}, session: {}", tenantId, sessionId);
                } catch (Exception e) {
                    logger.error("Failed to process conversation turn for tenant: {}, session: {}", tenantId, sessionId, e);
                    // Don't fail the request if memory storage fails
                }
                
                // Track usage
                trackUsage(tenantId, sessionId, request.getModel(), context.length(), response.length());
                
                emitter.complete();
                
            } catch (IOException e) {
                logger.error("Error in streaming response", e);
                emitter.completeWithError(e);
            } catch (InterruptedException e) {
                logger.error("Streaming interrupted", e);
                Thread.currentThread().interrupt();
                emitter.completeWithError(e);
            } catch (Exception e) {
                logger.error("Unexpected error in streaming", e);
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }
    
    /**
     * Create a non-streaming chat completion response.
     */
    private ChatCompletionResponse createNonStreamingResponse(ChatCompletionRequest request) {
        try {
            // Extract tenant and session from request context
            String tenantId = extractTenantId();
            String sessionId = extractSessionId(request);
            
            // Build context using ContextBuilderService
            String context = contextBuilderService.buildContext(
                tenantId, 
                sessionId, 
                getLastUserMessage(request), 
                null
            );
            
            // Create enhanced request with context
            ChatCompletionRequest enhancedRequest = enhanceRequestWithContext(request, context);
            
            // Call LLM service
            ChatCompletionResponse response = llmClientService.createChatCompletion(enhancedRequest);
            
            // Extract the assistant's response for memory storage
            String assistantResponse = extractAssistantResponse(response);
            
            // Store conversation and extract memories
            try {
                String userMessage = getLastUserMessage(request);
                Map<String, Object> contextMetadata = Map.of("context_length", context.length(), "model", request.getModel());

                logger.error("About to call memory storage service for tenant: {}, session: {}", tenantId, sessionId);
                memoryStorageService.processConversationTurn(
                    tenantId, sessionId, "default-user", userMessage, assistantResponse, contextMetadata);

                logger.error("Successfully processed conversation turn for tenant: {}, session: {}", tenantId, sessionId);
            } catch (Exception e) {
                logger.error("Failed to process conversation turn for tenant: {}, session: {}", tenantId, sessionId, e);
                // Don't fail the request if memory storage fails
            }
            
            // Track usage
            trackUsage(tenantId, sessionId, request.getModel(), context.length(), 
                      response.getUsage() != null ? response.getUsage().getCompletionTokens() : 0);
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error creating non-streaming response", e);
            // Return a fallback response
            return ChatCompletionResponse.builder()
                .id("chatcmpl-" + System.currentTimeMillis())
                .object("chat.completion")
                .created(System.currentTimeMillis() / 1000)
                .model(request.getModel())
                .choices(List.of(new ChatChoice(
                    0,
                    new ChatMessage("assistant", "I apologize, but I encountered an error processing your request. Please try again."),
                    "stop"
                )))
                .usage(new TokenUsage(0, 0, 0))
                .build();
        }
    }
    
    /**
     * Enhance the request with context information.
     */
    private ChatCompletionRequest enhanceRequestWithContext(ChatCompletionRequest originalRequest, String context) {
        // Create a new list with the context as a system message
        List<ChatMessage> enhancedMessages = new java.util.ArrayList<>();
        
        // Add context as system message
        if (context != null && !context.trim().isEmpty()) {
            enhancedMessages.add(new ChatMessage("system", 
                "You are a knowledge-aware AI assistant. Use the following context to provide accurate and helpful responses:\n\n" + context));
        }
        
        // Add original messages
        enhancedMessages.addAll(originalRequest.getMessages());
        
        // Create enhanced request
        return ChatCompletionRequest.builder()
            .model(originalRequest.getModel())
            .messages(enhancedMessages)
            .temperature(originalRequest.getTemperature())
            .maxTokens(originalRequest.getMaxTokens())
            .stream(originalRequest.getStream())
            .build();
    }
    
    /**
     * Extract tenant ID from security context.
     */
    private String extractTenantId() {
        // TODO: Extract from security context when available
        // For now, return a default tenant
        return "default-tenant";
    }
    
    /**
     * Extract session ID from request or generate one.
     */
    private String extractSessionId(ChatCompletionRequest request) {
        // TODO: Extract from request headers or generate based on user
        // For now, generate a session ID based on timestamp
        return "session-" + System.currentTimeMillis();
    }
    
    /**
     * Get the last user message from the request.
     */
    private String getLastUserMessage(ChatCompletionRequest request) {
        for (int i = request.getMessages().size() - 1; i >= 0; i--) {
            ChatMessage message = request.getMessages().get(i);
            if ("user".equals(message.getRole())) {
                return message.getContent();
            }
        }
        return "Hello"; // Fallback
    }
    
    /**
     * Extract the assistant's response content from the chat completion response
     */
    private String extractAssistantResponse(ChatCompletionResponse response) {
        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            ChatChoice choice = response.getChoices().get(0);
            if (choice.getMessage() != null) {
                return choice.getMessage().getContent();
            }
        }
        return "";
    }
    
    /**
     * Track usage for the request.
     */
    private void trackUsage(String tenantId, String sessionId, String model, int inputTokens, int outputTokens) {
        try {
            String userId = "default-user"; // TODO: Extract from security context
            String requestId = "req-" + System.currentTimeMillis();
            int knowledgeTokens = 0; // TODO: Calculate from context building
            double costEstimate = 0.0; // TODO: Calculate based on model and tokens
            
            usageTrackingService.trackChatCompletion(tenantId, userId, sessionId, requestId, 
                model, inputTokens, outputTokens, knowledgeTokens, costEstimate);
        } catch (Exception e) {
            logger.warn("Failed to track usage for tenant: {}, session: {}", tenantId, sessionId, e);
        }
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

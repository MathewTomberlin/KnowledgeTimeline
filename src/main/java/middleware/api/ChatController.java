package middleware.api;

import middleware.dto.ChatCompletionRequest;
import middleware.dto.ChatCompletionResponse;
import middleware.service.LLMClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for chat completion endpoints.
 * Provides OpenAI-compatible chat completion functionality.
 */
@RestController
@RequestMapping("/v1")
public class ChatController {
    
    @Autowired
    private LLMClientService llmClientService;
    
    /**
     * Create a chat completion.
     * 
     * @param request The chat completion request
     * @return The chat completion response
     */
    @PostMapping("/chat/completions")
    public ResponseEntity<ChatCompletionResponse> createChatCompletion(@RequestBody ChatCompletionRequest request) {
        try {
            ChatCompletionResponse response = llmClientService.createChatCompletion(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // In a real implementation, you would have proper error handling
            throw new RuntimeException("Failed to create chat completion", e);
        }
    }
}

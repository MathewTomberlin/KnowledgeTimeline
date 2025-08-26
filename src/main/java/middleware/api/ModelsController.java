package middleware.api;

import middleware.dto.Model;
import middleware.service.LLMClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for model-related endpoints.
 * Provides OpenAI-compatible model listing functionality.
 */
@RestController
@RequestMapping("/v1")
public class ModelsController {
    
    @Autowired
    private LLMClientService llmClientService;
    
    /**
     * Get available models.
     * 
     * @return List of available models
     */
    @GetMapping("/models")
    public ResponseEntity<Map<String, Object>> getModels() {
        List<Model> models = llmClientService.getAvailableModels();
        
        Map<String, Object> response = new HashMap<>();
        response.put("object", "list");
        response.put("data", models);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Health check endpoint.
     * 
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("timestamp", System.currentTimeMillis());
        health.put("llm_service", llmClientService.isHealthy());
        
        return ResponseEntity.ok(health);
    }
}

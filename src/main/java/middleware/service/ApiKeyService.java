package middleware.service;

import middleware.model.ApiKey;

/**
 * Service interface for API key management and validation.
 */
public interface ApiKeyService {
    
    /**
     * Validate an API key and return the associated key entity if valid.
     * 
     * @param apiKey The API key to validate
     * @return The ApiKey entity if valid, null otherwise
     */
    ApiKey validateApiKey(String apiKey);
    
    /**
     * Create a new API key for a tenant.
     * 
     * @param tenantId The tenant ID
     * @param name The name for the API key
     * @return The generated API key (plain text, not hashed)
     */
    String createApiKey(String tenantId, String name);
    
    /**
     * Deactivate an API key.
     * 
     * @param apiKeyId The API key ID to deactivate
     * @return true if deactivated successfully, false otherwise
     */
    boolean deactivateApiKey(String apiKeyId);
    
    /**
     * Check if an API key is valid and active.
     * 
     * @param apiKey The API key to check
     * @return true if valid and active, false otherwise
     */
    boolean isApiKeyValid(String apiKey);
}

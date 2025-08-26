package middleware.service.impl;

import middleware.model.ApiKey;
import middleware.repository.ApiKeyRepository;
import middleware.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Implementation of ApiKeyService for API key management and validation.
 */
@Service
public class ApiKeyServiceImpl implements middleware.service.ApiKeyService {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiKeyServiceImpl.class);
    private static final int API_KEY_LENGTH = 32;
    
    @Autowired
    private ApiKeyRepository apiKeyRepository;
    
    @Autowired
    private TenantRepository tenantRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public ApiKey validateApiKey(String apiKey) {
        try {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                return null;
            }
            
            // Hash the provided API key
            String hashedKey = passwordEncoder.encode(apiKey);
            
            // Find the API key by hash
            Optional<ApiKey> foundKey = apiKeyRepository.findByKeyHash(hashedKey);
            
            if (foundKey.isPresent()) {
                ApiKey key = foundKey.get();
                
                // Check if key is active
                if (key.isActive()) {
                    // Update last used timestamp
                    key.setLastUsedAt(LocalDateTime.now());
                    apiKeyRepository.save(key);
                    return key;
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.error("Error validating API key", e);
            return null;
        }
    }
    
    @Override
    public String createApiKey(String tenantId, String name) {
        try {
            // Verify tenant exists
            if (!tenantRepository.existsByTenantId(tenantId)) {
                throw new IllegalArgumentException("Tenant not found: " + tenantId);
            }
            
            // Generate a new API key
            String plainTextKey = generateApiKey();
            
            // Hash the API key
            String hashedKey = passwordEncoder.encode(plainTextKey);
            
            // Create and save the API key entity
            ApiKey apiKey = new ApiKey();
            apiKey.setKeyHash(hashedKey);
            apiKey.setTenantId(tenantId);
            apiKey.setName(name);
            apiKey.setActive(true);
            apiKey.setCreatedAt(LocalDateTime.now());
            
            apiKeyRepository.save(apiKey);
            
            logger.info("Created new API key for tenant: {}", tenantId);
            
            // Return the plain text key (this is the only time it's available)
            return plainTextKey;
        } catch (Exception e) {
            logger.error("Error creating API key for tenant: {}", tenantId, e);
            throw new RuntimeException("Failed to create API key", e);
        }
    }
    
    @Override
    public boolean deactivateApiKey(String apiKeyId) {
        try {
            Optional<ApiKey> apiKey = apiKeyRepository.findById(apiKeyId);
            if (apiKey.isPresent()) {
                ApiKey key = apiKey.get();
                key.setActive(false);
                apiKeyRepository.save(key);
                logger.info("Deactivated API key: {}", apiKeyId);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Error deactivating API key: {}", apiKeyId, e);
            return false;
        }
    }
    
    @Override
    public boolean isApiKeyValid(String apiKey) {
        return validateApiKey(apiKey) != null;
    }
    
    /**
     * Generate a cryptographically secure API key.
     */
    private String generateApiKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[API_KEY_LENGTH];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

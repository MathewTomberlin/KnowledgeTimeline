package middleware.service.impl;

import middleware.model.ApiKey;
import middleware.model.Tenant;
import middleware.repository.ApiKeyRepository;
import middleware.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for ApiKeyServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class ApiKeyServiceImplTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ApiKeyServiceImpl apiKeyService;

    private ApiKey testApiKey;
    private Tenant testTenant;
    private String testPlainTextKey;
    private String testHashedKey;

    @BeforeEach
    void setUp() {
        testPlainTextKey = "test-api-key-12345";
        testHashedKey = "hashed-key-12345";
        
        testTenant = new Tenant();
        testTenant.setTenantId("tenant1");
        testTenant.setName("Test Tenant");
        testTenant.setActive(true);
        
        testApiKey = new ApiKey();
        testApiKey.setId("key1");
        testApiKey.setKeyHash(testHashedKey);
        testApiKey.setTenantId("tenant1");
        testApiKey.setName("Test API Key");
        testApiKey.setActive(true);
        testApiKey.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testValidateApiKey_ValidKey() {
        // Given
        when(passwordEncoder.encode(testPlainTextKey)).thenReturn(testHashedKey);
        when(apiKeyRepository.findByKeyHash(testHashedKey)).thenReturn(Optional.of(testApiKey));
        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(testApiKey);

        // When
        ApiKey result = apiKeyService.validateApiKey(testPlainTextKey);

        // Then
        assertNotNull(result);
        assertEquals(testApiKey.getId(), result.getId());
        verify(apiKeyRepository).save(any(ApiKey.class));
    }

    @Test
    void testValidateApiKey_InvalidKey() {
        // Given
        when(passwordEncoder.encode(testPlainTextKey)).thenReturn(testHashedKey);
        when(apiKeyRepository.findByKeyHash(testHashedKey)).thenReturn(Optional.empty());

        // When
        ApiKey result = apiKeyService.validateApiKey(testPlainTextKey);

        // Then
        assertNull(result);
        verify(apiKeyRepository, never()).save(any(ApiKey.class));
    }

    @Test
    void testValidateApiKey_InactiveKey() {
        // Given
        testApiKey.setActive(false);
        when(passwordEncoder.encode(testPlainTextKey)).thenReturn(testHashedKey);
        when(apiKeyRepository.findByKeyHash(testHashedKey)).thenReturn(Optional.of(testApiKey));

        // When
        ApiKey result = apiKeyService.validateApiKey(testPlainTextKey);

        // Then
        assertNull(result);
        verify(apiKeyRepository, never()).save(any(ApiKey.class));
    }

    @Test
    void testValidateApiKey_NullKey() {
        // When
        ApiKey result = apiKeyService.validateApiKey(null);

        // Then
        assertNull(result);
        verify(apiKeyRepository, never()).findByKeyHash(anyString());
    }

    @Test
    void testValidateApiKey_EmptyKey() {
        // When
        ApiKey result = apiKeyService.validateApiKey("");

        // Then
        assertNull(result);
        verify(apiKeyRepository, never()).findByKeyHash(anyString());
    }

    @Test
    void testCreateApiKey_Success() {
        // Given
        String tenantId = "tenant1";
        String name = "New API Key";
        when(tenantRepository.existsByTenantId(tenantId)).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-key");
        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(testApiKey);

        // When
        String result = apiKeyService.createApiKey(tenantId, name);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(tenantRepository).existsByTenantId(tenantId);
        verify(apiKeyRepository).save(any(ApiKey.class));
    }

    @Test
    void testCreateApiKey_TenantNotFound() {
        // Given
        String tenantId = "nonexistent";
        String name = "New API Key";
        when(tenantRepository.existsByTenantId(tenantId)).thenReturn(false);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            apiKeyService.createApiKey(tenantId, name);
        });
        verify(apiKeyRepository, never()).save(any(ApiKey.class));
    }

    @Test
    void testDeactivateApiKey_Success() {
        // Given
        String keyId = "key1";
        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(testApiKey));
        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(testApiKey);

        // When
        boolean result = apiKeyService.deactivateApiKey(keyId);

        // Then
        assertTrue(result);
        verify(apiKeyRepository).save(argThat(key -> !key.isActive()));
    }

    @Test
    void testDeactivateApiKey_KeyNotFound() {
        // Given
        String keyId = "nonexistent";
        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.empty());

        // When
        boolean result = apiKeyService.deactivateApiKey(keyId);

        // Then
        assertFalse(result);
        verify(apiKeyRepository, never()).save(any(ApiKey.class));
    }

    @Test
    void testIsApiKeyValid_ValidKey() {
        // Given
        when(passwordEncoder.encode(testPlainTextKey)).thenReturn(testHashedKey);
        when(apiKeyRepository.findByKeyHash(testHashedKey)).thenReturn(Optional.of(testApiKey));
        when(apiKeyRepository.save(any(ApiKey.class))).thenReturn(testApiKey);

        // When
        boolean result = apiKeyService.isApiKeyValid(testPlainTextKey);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsApiKeyValid_InvalidKey() {
        // Given
        when(passwordEncoder.encode(testPlainTextKey)).thenReturn(testHashedKey);
        when(apiKeyRepository.findByKeyHash(testHashedKey)).thenReturn(Optional.empty());

        // When
        boolean result = apiKeyService.isApiKeyValid(testPlainTextKey);

        // Then
        assertFalse(result);
    }

    @Test
    void testValidateApiKey_ExceptionHandling() {
        // Given
        when(passwordEncoder.encode(testPlainTextKey)).thenThrow(new RuntimeException("Encoder error"));

        // When
        ApiKey result = apiKeyService.validateApiKey(testPlainTextKey);

        // Then
        assertNull(result);
    }

    @Test
    void testCreateApiKey_ExceptionHandling() {
        // Given
        String tenantId = "tenant1";
        String name = "New API Key";
        when(tenantRepository.existsByTenantId(tenantId)).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenThrow(new RuntimeException("Encoder error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            apiKeyService.createApiKey(tenantId, name);
        });
    }
}

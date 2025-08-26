package middleware.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import middleware.model.ApiKey;
import middleware.model.Tenant;
import middleware.repository.TenantRepository;
import middleware.service.ApiKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for ApiKeyAuthenticationFilter.
 */
@ExtendWith(MockitoExtension.class)
class ApiKeyAuthenticationFilterTest {

    @Mock
    private ApiKeyService apiKeyService;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private ApiKeyAuthenticationFilter filter;
    private ApiKey testApiKey;
    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        filter = new ApiKeyAuthenticationFilter(apiKeyService, tenantRepository);
        
        testTenant = new Tenant();
        testTenant.setTenantId("tenant1");
        testTenant.setName("Test Tenant");
        testTenant.setActive(true);
        
        testApiKey = new ApiKey();
        testApiKey.setId("key1");
        testApiKey.setTenantId("tenant1");
        testApiKey.setActive(true);
        
        // Clear security context
        SecurityContextHolder.setContext(new SecurityContextImpl());
    }

    @Test
    void testDoFilterInternal_ValidApiKey() throws Exception {
        // Given
        String validApiKey = "valid-api-key-12345";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validApiKey);
        when(apiKeyService.validateApiKey(validApiKey)).thenReturn(testApiKey);
        when(tenantRepository.findByTenantId("tenant1")).thenReturn(Optional.of(testTenant));

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(request).setAttribute("tenantId", "tenant1");
        verify(request).setAttribute("apiKey", testApiKey);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_NoApiKey() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(request, never()).setAttribute(anyString(), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_InvalidApiKey() throws Exception {
        // Given
        String invalidApiKey = "invalid-api-key";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidApiKey);
        when(apiKeyService.validateApiKey(invalidApiKey)).thenReturn(null);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(request, never()).setAttribute(anyString(), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_InactiveApiKey() throws Exception {
        // Given
        String apiKey = "inactive-api-key";
        testApiKey.setActive(false);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + apiKey);
        when(apiKeyService.validateApiKey(apiKey)).thenReturn(testApiKey);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(request, never()).setAttribute(anyString(), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_TenantNotFound() throws Exception {
        // Given
        String apiKey = "valid-api-key";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + apiKey);
        when(apiKeyService.validateApiKey(apiKey)).thenReturn(testApiKey);
        when(tenantRepository.findByTenantId("tenant1")).thenReturn(Optional.empty());

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(request, never()).setAttribute(anyString(), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_InactiveTenant() throws Exception {
        // Given
        String apiKey = "valid-api-key";
        testTenant.setActive(false);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + apiKey);
        when(apiKeyService.validateApiKey(apiKey)).thenReturn(testApiKey);
        when(tenantRepository.findByTenantId("tenant1")).thenReturn(Optional.of(testTenant));

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(request, never()).setAttribute(anyString(), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_NoBearerPrefix() throws Exception {
        // Given
        String apiKey = "valid-api-key";
        when(request.getHeader("Authorization")).thenReturn(apiKey);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(apiKeyService, never()).validateApiKey(anyString());
        verify(request, never()).setAttribute(anyString(), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_EmptyBearerToken() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer ");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(apiKeyService, never()).validateApiKey(anyString());
        verify(request, never()).setAttribute(anyString(), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testDoFilterInternal_ExceptionHandling() throws Exception {
        // Given
        String apiKey = "valid-api-key";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + apiKey);
        when(apiKeyService.validateApiKey(apiKey)).thenThrow(new RuntimeException("Service error"));

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(request, never()).setAttribute(anyString(), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testExtractApiKey_ValidHeader() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer test-key-123");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(apiKeyService).validateApiKey("test-key-123");
    }

    @Test
    void testExtractApiKey_WhitespaceInHeader() throws Exception {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer  test-key-123  ");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(apiKeyService).validateApiKey(" test-key-123  ");
    }
}

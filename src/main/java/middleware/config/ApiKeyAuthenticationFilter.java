package middleware.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import middleware.model.ApiKey;
import middleware.model.Tenant;
import middleware.repository.TenantRepository;
import middleware.service.ApiKeyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Filter for API key authentication.
 * Extracts API key from request headers and validates it.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);
    private static final String API_KEY_HEADER = "Authorization";
    private static final String API_KEY_PREFIX = "Bearer ";
    
    private final ApiKeyService apiKeyService;
    private final TenantRepository tenantRepository;
    
    public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService, TenantRepository tenantRepository) {
        this.apiKeyService = apiKeyService;
        this.tenantRepository = tenantRepository;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String apiKey = extractApiKey(request);
            
            if (apiKey != null) {
                ApiKey key = apiKeyService.validateApiKey(apiKey);
                if (key != null && key.isActive()) {
                    // Fetch the tenant using the tenantId from the API key
                    var tenantOpt = tenantRepository.findByTenantId(key.getTenantId());
                    if (tenantOpt.isPresent()) {
                        Tenant tenant = tenantOpt.get();
                        if (tenant.isActive()) {
                            // Create authentication token with tenant context
                            UsernamePasswordAuthenticationToken authentication = 
                                new UsernamePasswordAuthenticationToken(
                                    key, 
                                    null, 
                                    Collections.emptyList()
                                );
                            
                            // Set tenant context in request attributes for later use
                            request.setAttribute("tenantId", tenant.getTenantId());
                            request.setAttribute("apiKey", key);
                            
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            logger.debug("Authenticated request for tenant: {}", tenant.getTenantId());
                        } else {
                            logger.warn("Inactive tenant for API key: {}", key.getId());
                        }
                    } else {
                        logger.warn("Tenant not found for API key: {}", key.getId());
                    }
                } else {
                    logger.warn("Invalid or inactive API key: {}", apiKey);
                }
            } else {
                logger.debug("No API key provided in request");
            }
        } catch (Exception e) {
            logger.error("Error processing API key authentication", e);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String extractApiKey(HttpServletRequest request) {
        String authHeader = request.getHeader(API_KEY_HEADER);
        
        if (authHeader != null && authHeader.startsWith(API_KEY_PREFIX)) {
            String token = authHeader.substring(API_KEY_PREFIX.length());
            // Return null if the token is empty or only whitespace
            return token.trim().isEmpty() ? null : token;
        }
        
        return null;
    }
}

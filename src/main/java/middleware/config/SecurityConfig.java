package middleware.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Security configuration that completely disables Spring Security for local development.
 * This configuration only applies when spring.security.enabled=false is set.
 */
@Configuration
@Profile("local")
@ConditionalOnProperty(name = "spring.security.enabled", havingValue = "false", matchIfMissing = true)
public class SecurityConfig {
    // This class exists solely to disable Spring Security for local development
    // No @EnableWebSecurity annotation means no security filters are applied
}

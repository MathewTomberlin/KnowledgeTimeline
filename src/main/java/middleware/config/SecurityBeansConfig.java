package middleware.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration for security-related beans.
 * Separated from SecurityConfig to avoid circular dependencies.
 */
@Configuration
public class SecurityBeansConfig {

    /**
     * Password encoder bean for hashing API keys and other sensitive data.
     * Temporarily using NoOpPasswordEncoder for testing purposes.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // TODO: Change back to BCryptPasswordEncoder() for production
        // return new BCryptPasswordEncoder();
        return org.springframework.security.crypto.password.NoOpPasswordEncoder.getInstance();
    }
}

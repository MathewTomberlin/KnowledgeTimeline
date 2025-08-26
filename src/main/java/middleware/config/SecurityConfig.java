package middleware.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the Knowledge Middleware application.
 * Provides different security settings for different profiles.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Security configuration for local development and testing.
     * Disables authentication to allow easy API testing.
     */
    @Bean
    @Profile({"local", "test", "docker"})
    public SecurityFilterChain localSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/**").permitAll()
            )
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable);
        
        return http.build();
    }

    /**
     * Security configuration for production environments.
     * Implements proper authentication and authorization.
     */
    @Bean
    @Profile({"gcp", "production"})
    public SecurityFilterChain productionSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/v1/**").authenticated()
                .requestMatchers("/jobs/**").authenticated()
                .anyRequest().authenticated()
            );
        
        // TODO: Add API key authentication filter
        // TODO: Add rate limiting
        // TODO: Add CORS configuration
        
        return http.build();
    }
}

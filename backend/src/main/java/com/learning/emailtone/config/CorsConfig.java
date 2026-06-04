package com.learning.emailtone.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS for the React frontend.
 *
 * <p>WHY a custom config rather than {@code @CrossOrigin} on each controller:
 * the allowed origin is environment-dependent (dev uses Vite's localhost:5173;
 * production uses your real domain), so it should come from configuration.
 * One bean = one source of truth.
 *
 * <p>WHY restrict the origin instead of using "*": this API ultimately calls
 * a paid third-party LLM. An open CORS policy lets any random page in any
 * browser tab make requests on behalf of the user. Pin the allowed origin.
 */
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer(
            @Value("${email-tone.cors.allowed-origins}") String allowedOrigins) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(allowedOrigins.split(","))
                        .allowedMethods("GET", "POST", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(false)
                        .maxAge(3600);
            }
        };
    }
}

package com.prepcreatine.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration — single source of truth (SecurityConfig delegates here).
 * Origins are read from the cors.allowed-origins env var (comma-separated).
 * Credentials must be true so the httpOnly JWT cookie is forwarded.
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Merge env-var origins with hard-coded production domain
        List<String> origins = new ArrayList<>(Arrays.asList(allowedOrigins.split(",")));
        if (!origins.contains("https://prepcreatine.com")) {
            origins.add("https://prepcreatine.com");
        }
        config.setAllowedOrigins(origins);

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Authorization", "X-Request-ID", "Accept"));
        config.setExposedHeaders(List.of("X-Request-ID"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Register for ALL paths so OAuth2 redirect and api/** both get CORS headers
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

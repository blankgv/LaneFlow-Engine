package com.laneflow.engine.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String allowedOrigins,
            @Value("${app.cors.allowed-origin-patterns:}") String allowedOriginPatterns,
            @Value("${app.cors.allowed-methods}") String allowedMethods,
            @Value("${app.cors.allowed-headers}") String allowedHeaders,
            @Value("${app.cors.allow-credentials}") boolean allowCredentials
    ) {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = splitCsv(allowedOrigins);
        List<String> originPatterns = splitCsv(allowedOriginPatterns);

        if (!origins.isEmpty()) {
            configuration.setAllowedOrigins(origins);
        }

        if (!originPatterns.isEmpty()) {
            configuration.setAllowedOriginPatterns(originPatterns);
        }

        configuration.setAllowedMethods(requireValues("app.cors.allowed-methods", allowedMethods));
        configuration.setAllowedHeaders(requireValues("app.cors.allowed-headers", allowedHeaders));
        configuration.setAllowCredentials(allowCredentials);

        if (origins.isEmpty() && originPatterns.isEmpty()) {
            throw new IllegalStateException("CORS requires app.cors.allowed-origins or app.cors.allowed-origin-patterns to be configured.");
        }

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private List<String> requireValues(String propertyName, String value) {
        List<String> parsed = splitCsv(value);
        if (parsed.isEmpty()) {
            throw new IllegalStateException("Missing required CORS property: " + propertyName);
        }
        return parsed;
    }
}

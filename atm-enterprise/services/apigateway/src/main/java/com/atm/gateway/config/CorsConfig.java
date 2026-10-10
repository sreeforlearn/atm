package com.atm.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Dedicated CORS configuration for the API Gateway.
 *
 * <p>Provides a {@link CorsWebFilter} bean that applies CORS headers
 * globally to all reactive responses. This works alongside the
 * {@code globalcors} block in {@code application.yml}.</p>
 */
@Configuration
public class CorsConfig {

    /**
     * Reactive {@link CorsWebFilter} allowing all origins/methods/headers.
     *
     * @return configured CORS web filter
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }

}

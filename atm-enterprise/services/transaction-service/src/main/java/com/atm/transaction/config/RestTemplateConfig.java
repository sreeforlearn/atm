package com.atm.transaction.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Provides a configured {@link RestTemplate} bean used by the
 * {@link com.atm.transaction.service.AccountServiceClient}.
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Build a RestTemplate with sensible timeouts.
     *
     * @param builder the RestTemplateBuilder
     * @return configured RestTemplate
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .build();
    }
}

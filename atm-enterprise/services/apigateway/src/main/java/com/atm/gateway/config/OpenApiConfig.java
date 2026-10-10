package com.atm.gateway.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI configuration for the API Gateway.
 *
 * <p>Provides a single entry point that aggregates the Swagger UI for all
 * downstream services.</p>
 */
@Configuration
public class OpenApiConfig {

    /**
     * OpenAPI definition with JWT bearer security scheme.
     */
    @Bean
    public OpenAPI gatewayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ATM Enterprise - API Gateway")
                        .description("Single entry point for all ATM Enterprise microservices. "
                                + "Routes to auth, account, transaction and card services.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ATM Enterprise Team")
                                .email("devops@atm-enterprise.com"))
                        .license(new License()
                                .name("Proprietary")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Provide the JWT issued by /api/auth/login")));
    }
}

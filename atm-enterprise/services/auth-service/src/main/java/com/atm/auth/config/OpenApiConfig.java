package com.atm.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger configuration for the auth-service.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Customized OpenAPI definition with JWT bearer security scheme.
     *
     * @return OpenAPI bean
     */
    @Bean
    public OpenAPI atmAuthOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ATM Enterprise - Auth Service API")
                        .description("Card authentication, PIN management and audit trail for the ATM Enterprise platform")
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

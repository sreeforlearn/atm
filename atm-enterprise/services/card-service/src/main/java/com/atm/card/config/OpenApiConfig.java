package com.atm.card.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Springdoc OpenAPI configuration for the card-service.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Build the OpenAPI metadata bean.
     *
     * @return configured OpenAPI descriptor
     */
    @Bean
    public OpenAPI cardServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ATM Enterprise Card Service API")
                        .description("REST API for card lifecycle management (issue, block, limits)")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ATM Enterprise Team")
                                .email("atm-platform@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}

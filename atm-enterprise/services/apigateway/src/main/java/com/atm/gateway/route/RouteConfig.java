package com.atm.gateway.route;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Programmatic route definitions (in addition to those declared in
 * {@code application.yml}).
 *
 * <p>This is intentionally minimal because the primary route configuration is
 * declarative in YAML. The bean exists so that downstream service URLs can be
 * overridden via environment variables and so that the {@code route} package
 * is non-empty for clarity. Routes registered here are merged with those
 * defined in YAML.</p>
 */
@Configuration
public class RouteConfig {

    @Value("${AUTH_SERVICE_URL:http://auth-service:8081}")
    private String authServiceUrl;

    @Value("${ACCOUNT_SERVICE_URL:http://account-service:8082}")
    private String accountServiceUrl;

    @Value("${TRANSACTION_SERVICE_URL:http://transaction-service:8083}")
    private String transactionServiceUrl;

    @Value("${CARD_SERVICE_URL:http://card-service:8084}")
    private String cardServiceUrl;

    /**
     * Define routes programmatically. Equivalent to the YAML routes.
     *
     * @param builder Spring Cloud Gateway's {@link RouteLocatorBuilder}
     * @return route locator
     */
    @Bean
    public RouteLocator atmRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-route-java", r -> r
                        .path("/api/auth/**")
                        .uri(authServiceUrl))
                .route("account-route-java", r -> r
                        .path("/api/accounts/**")
                        .uri(accountServiceUrl))
                .route("transaction-route-java", r -> r
                        .path("/api/transactions/**")
                        .uri(transactionServiceUrl))
                .route("card-route-java", r -> r
                        .path("/api/cards/**")
                        .uri(cardServiceUrl))
                .build();
    }
}

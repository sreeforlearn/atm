package com.atm.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Global JWT authentication filter.
 *
 * <p>Runs with the highest precedence (order = -1). Extracts the Bearer token
 * from the {@code Authorization} header, validates it against the shared JWT
 * secret and (on success) injects an {@code X-Card-Number} header into the
 * downstream request so backend services can identify the caller.</p>
 *
 * <p>Public endpoints ({@code /api/auth/login}, {@code /api/auth/register},
 * Swagger UI, actuator) are allowed through without a token.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CARD_HEADER = "X-Card-Number";

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/swagger-ui",
            "/v3/api-docs",
            "/actuator"
    );

    private final JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // Allow public paths
        if (isPublic(path)) {
            return chain.filter(exchange);
        }

        // Allow CORS preflight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod().name())) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(AUTH_HEADER);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        if (!jwtUtil.validateToken(token)) {
            return unauthorized(exchange, "Invalid or expired JWT");
        }

        String cardNumber;
        try {
            cardNumber = jwtUtil.extractCardNumber(token);
        } catch (Exception ex) {
            log.warn("Failed to extract card number from JWT: {}", ex.getMessage());
            return unauthorized(exchange, "Invalid JWT claims");
        }

        log.debug("Authenticated card {} for path {}", cardNumber, path);

        // Mutate request to add card number header downstream
        ServerHttpRequest mutated = request.mutate()
                .header(CARD_HEADER, cardNumber)
                .header("X-Forwarded-For", clientIp(request))
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        return -1; // Highest precedence
    }

    /**
     * Reject the request with a 401 response.
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        log.warn("Gateway rejecting request: {}", message);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"success\":false,\"message\":\"" + message + "\"}";
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    /**
     * Check whether the path is publicly accessible.
     */
    private boolean isPublic(String path) {
        for (String prefix : PUBLIC_PATHS) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolve the client IP, preferring X-Forwarded-For.
     */
    private String clientIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }
}

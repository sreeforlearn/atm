package com.atm.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter.
 *
 * <p>Each client IP gets a bucket of {@code app.rate-limit.requests-per-minute}
 * requests per minute. Buckets reset on the minute boundary.</p>
 *
 * <p>This is a single-instance limiter suitable for development. For
 * production deployments, replace with a Redis-backed rate limiter.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final String X_FORWARD = "X-Forwarded-For";

    @Value("${app.rate-limit.requests-per-minute:100}")
    private int requestsPerMinute;

    /** Bounded cache keyed by client IP. */
    private final Map<String, IpBucket> buckets = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Skip preflight CORS
        if ("OPTIONS".equalsIgnoreCase(request.getMethod().name())) {
            return chain.filter(exchange);
        }

        // Skip rate limiting for health checks & swagger
        String path = request.getPath().value();
        if (path.startsWith("/actuator") || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")) {
            return chain.filter(exchange);
        }

        String ip = resolveClientIp(request);
        IpBucket bucket = buckets.computeIfAbsent(ip, k -> new IpBucket());

        int count = bucket.incrementAndGet();
        if (count > requestsPerMinute) {
            log.warn("Rate limit exceeded for {}: {} req/min", ip, count);
            return rejectTooManyRequests(exchange);
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -2; // Run before the JWT filter
    }

    /**
     * Reject with HTTP 429 Too Many Requests.
     */
    private Mono<Void> rejectTooManyRequests(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"success\":false,\"message\":\"Rate limit exceeded. Try again later.\"}";
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    /**
     * Resolve the client IP, preferring X-Forwarded-For.
     */
    private String resolveClientIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst(X_FORWARD);
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }

    /**
     * Per-IP bucket with a sliding-minute counter.
     */
    private static class IpBucket {
        private final AtomicInteger counter = new AtomicInteger(0);
        private volatile long windowStart;

        IpBucket() {
            this.windowStart = System.currentTimeMillis();
        }

        int incrementAndGet() {
            long now = System.currentTimeMillis();
            // Reset every 60 seconds
            if (now - windowStart >= 60_000L) {
                counter.set(0);
                windowStart = now;
            }
            return counter.incrementAndGet();
        }
    }
}

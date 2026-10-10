package com.atm.auth.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Filter that runs once per request, extracts a Bearer JWT from the
 * {@code Authorization} header, validates it, and populates the
 * {@link org.springframework.security.core.context.SecurityContext} so downstream
 * controllers can rely on the authenticated principal.
 *
 * <p>The principal's username is the 16-digit card number, which the controllers
 * can retrieve via {@code authentication.getName()}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ROLE_CARDHOLDER = "ROLE_CARDHOLDER";

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            try {
                String cardNumber = jwtUtil.extractCardNumber(token);
                // Build a lightweight principal (the card number) so we don't
                // need to hit the database on every authenticated request.
                UserDetails principal = User.builder()
                        .username(cardNumber)
                        .password("n/a")  // not used - token already validated
                        .authorities(List.of(new SimpleGrantedAuthority(ROLE_CARDHOLDER)))
                        .build();
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        principal,
                        token,
                        principal.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
                // Expose card number downstream (for cross-service calls)
                request.setAttribute("cardNumber", cardNumber);
            } catch (Exception ex) {
                log.warn("JWT authentication failed: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}

package com.atm.account.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT validation utility for account-service. Mirrors the secret and signing
 * algorithm used by the auth-service so that tokens issued there are accepted here.
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.issuer:atm-enterprise}")
    private String issuer;

    private SecretKey key;

    @PostConstruct
    void init() {
        byte[] keyBytes = decodeKeyBytes(secret);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Decode the configured secret into raw key bytes.
     *
     * <p>Heuristic: if the secret looks like base64 (only contains
     * {@code [A-Za-z0-9+/=]} and length &gt; 24), decode it. Otherwise, treat it
     * as plain text.</p>
     */
    private static byte[] decodeKeyBytes(String secret) {
        if (secret.length() > 24 && secret.matches("^[A-Za-z0-9+/=]+$")) {
            try {
                return Decoders.BASE64.decode(secret);
            } catch (IllegalArgumentException ex) {
                // fall through
            }
        }
        return secret.getBytes();
    }

    /**
     * Validate a JWT signature + expiry.
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Invalid JWT: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Extract the card number (subject) from a JWT.
     */
    public String extractCardNumber(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Extract expiration date.
     */
    public Date extractExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

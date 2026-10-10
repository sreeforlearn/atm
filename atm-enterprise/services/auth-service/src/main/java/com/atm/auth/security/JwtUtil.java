package com.atm.auth.security;

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
import java.time.Instant;
import java.util.Date;

/**
 * JWT utility for token generation, validation and claim extraction.
 *
 * <p>Uses HS256 signed with an HMAC-SHA key derived from the configured secret.</p>
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms:3600000}")
    private long expirationMs;

    @Value("${app.jwt.issuer:atm-enterprise}")
    private String issuer;

    private SecretKey key;

    @PostConstruct
    void init() {
        byte[] keyBytes = decodeKeyBytes(secret);
        // Pad key to at least 256 bits if too short
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
                // fall through to plain bytes
            }
        }
        return secret.getBytes();
    }

    /**
     * Generate a signed JWT for the given card number subject.
     *
     * @param cardNumber subject (card number)
     * @return compact JWT string
     */
    public String generateToken(String cardNumber) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(cardNumber)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Validate a JWT's signature and expiry.
     *
     * @param token compact JWT
     * @return {@code true} if the token is valid
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
     *
     * @param token compact JWT
     * @return card number
     */
    public String extractCardNumber(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Extract expiration date.
     *
     * @param token compact JWT
     * @return expiration date
     */
    public Date extractExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    /**
     * Get the configured token lifetime in milliseconds.
     *
     * @return expiration in ms
     */
    public long getExpirationMs() {
        return expirationMs;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

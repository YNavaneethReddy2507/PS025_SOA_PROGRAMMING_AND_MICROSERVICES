package com.auction.auth.security;

import com.auction.auth.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    public String generateToken(Long userId, String email, String username, Role role) {
        return generateTokenWithExpiry(userId, email, username, role, this.expirationMs);
    }

    public String generateTokenWithExpiry(Long userId, String email, String username, Role role, long customExpirationMs) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role != null ? role.name() : Role.BUYER.name());

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + customExpirationMs);

        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(this.key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).get("username", String.class);
    }

    public Long extractUserId(String token) {
        return extractAllClaims(token).get("userId", Long.class);
    }

    public Role extractRole(String token) {
        String roleStr = extractAllClaims(token).get("role", String.class);
        return roleStr != null ? Role.valueOf(roleStr) : Role.BUYER;
    }

    public boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.debug("Token validation failed: token is missing or empty");
            return false;
        }
        try {
            Jwts.parser()
                    .verifyWith(this.key)
                    .build()
                    .parseSignedClaims(token.trim());
            return true;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("Token validation failed: token has expired");
            return false;
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.warn("Token validation failed: token is malformed");
            return false;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("Token validation failed: signature mismatch");
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token validation failed: invalid token");
            return false;
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}

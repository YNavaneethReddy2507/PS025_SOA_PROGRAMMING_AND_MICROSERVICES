package com.auction.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Collections;

/**
 * Reusable JWT Security Configuration & Helper Utilities.
 * <p>
 * Microservices requiring direct JWT validation (such as auction-service,
 * bidding-service, payment-service, or api-gateway) can import or adapt these
 * components to enforce token integrity, signature validation, and RBAC claims extraction.
 * </p>
 */
public class ReusableJwtSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(ReusableJwtSecurityConfig.class);

    /**
     * Stateless JWT Token Validator for standalone service-level validation.
     */
    public static class StandaloneJwtValidator {
        private final SecretKey key;

        public StandaloneJwtValidator(String base64Secret) {
            byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
            this.key = Keys.hmacShaKeyFor(keyBytes);
        }

        public boolean isValid(String token) {
            if (!StringUtils.hasText(token)) {
                return false;
            }
            try {
                Jwts.parser()
                        .verifyWith(this.key)
                        .build()
                        .parseSignedClaims(token.trim());
                return true;
            } catch (JwtException | IllegalArgumentException e) {
                // Tokens are intentionally NOT logged for security
                log.warn("Standalone JWT validation failed: {}", e.getClass().getSimpleName());
                return false;
            }
        }

        public Claims getClaims(String token) {
            return Jwts.parser()
                    .verifyWith(this.key)
                    .build()
                    .parseSignedClaims(token.trim())
                    .getPayload();
        }
    }

    /**
     * Reusable once-per-request filter for service-level token validation.
     */
    public static class ReusableJwtFilter extends OncePerRequestFilter {
        private final StandaloneJwtValidator validator;

        public ReusableJwtFilter(StandaloneJwtValidator validator) {
            this.validator = validator;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            String authHeader = request.getHeader("Authorization");
            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (validator.isValid(token)) {
                    Claims claims = validator.getClaims(token);
                    String email = claims.getSubject();
                    String role = String.valueOf(claims.get("role"));
                    Long userId = claims.get("userId", Long.class);

                    request.setAttribute("authenticatedUserId", userId);
                    request.setAttribute("authenticatedUserRole", role);

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
            filterChain.doFilter(request, response);
        }
    }
}

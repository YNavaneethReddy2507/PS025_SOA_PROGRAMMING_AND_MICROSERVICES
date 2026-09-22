package com.auction.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationFilterTest {

    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private AuthenticationFilter authenticationFilter;
    private JwtUtil jwtUtil;
    private RouteValidator routeValidator;

    @BeforeEach
    void setUp() {
        routeValidator = new RouteValidator();
        jwtUtil = new JwtUtil(SECRET);
        authenticationFilter = new AuthenticationFilter(routeValidator, jwtUtil);
    }

    private String generateValidToken(Long userId, String email, String role) {
        byte[] keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(SECRET);
        javax.crypto.SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);

        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(key)
                .compact();
    }

    @Test
    @DisplayName("Open endpoint passes through without Authorization header")
    void testOpenEndpointPassesWithoutAuth() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/auth/login").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, chain).block();

        assertTrue(chainCalled.get(), "Chain filter should be called for open endpoints");
        assertNull(exchange.getResponse().getStatusCode(), "Response status should remain unset (proceed downstream)");
    }

    @Test
    @DisplayName("Secured endpoint without Authorization header is rejected with 401")
    void testSecuredEndpointMissingAuthHeaderRejected() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/bids").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, chain).block();

        assertFalse(chainCalled.get(), "Chain should NOT be invoked for unauthenticated secured request");
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("Secured endpoint with malformed/invalid token is rejected with 401")
    void testSecuredEndpointInvalidTokenRejected() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/bids")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.signature")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, chain).block();

        assertFalse(chainCalled.get(), "Chain should NOT be invoked when token is invalid");
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("Secured endpoint with valid token injects X-User headers and forwards request")
    void testSecuredEndpointValidTokenInjectsHeaders() {
        String token = generateValidToken(42L, "bidder@example.com", "ROLE_BIDDER");
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/bids")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            capturedExchange.set(ex);
            return Mono.empty();
        };

        authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, chain).block();

        assertNotNull(capturedExchange.get(), "Chain should be invoked when token is valid");
        HttpHeaders forwardedHeaders = capturedExchange.get().getRequest().getHeaders();

        assertEquals("42", forwardedHeaders.getFirst("X-User-Id"));
        assertEquals("bidder@example.com", forwardedHeaders.getFirst("X-User-Email"));
        assertEquals("ROLE_BIDDER", forwardedHeaders.getFirst("X-User-Role"));
        // Ensure original Authorization header is preserved for defense-in-depth downstream validation
        assertEquals("Bearer " + token, forwardedHeaders.getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    @DisplayName("Client spoofed X-User headers are stripped on open and secured endpoints")
    void testClientSpoofedHeadersAreStripped() {
        // Test open endpoint with spoofed header
        MockServerHttpRequest openRequest = MockServerHttpRequest.post("/api/auth/login")
                .header("X-User-Id", "999")
                .header("X-User-Role", "ROLE_ADMIN")
                .build();
        ServerWebExchange openExchange = MockServerWebExchange.from(openRequest);

        AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            capturedExchange.set(ex);
            return Mono.empty();
        };

        authenticationFilter.apply(new AuthenticationFilter.Config()).filter(openExchange, chain).block();

        assertNotNull(capturedExchange.get());
        assertNull(capturedExchange.get().getRequest().getHeaders().getFirst("X-User-Id"),
                "Client-supplied X-User-Id must be stripped");
        assertNull(capturedExchange.get().getRequest().getHeaders().getFirst("X-User-Role"),
                "Client-supplied X-User-Role must be stripped");

        // Test secured endpoint with spoofed header overwritten by token claim
        String token = generateValidToken(42L, "bidder@example.com", "ROLE_BIDDER");
        MockServerHttpRequest securedRequest = MockServerHttpRequest.post("/api/bids")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-User-Id", "999")
                .header("X-User-Role", "ROLE_ADMIN")
                .build();
        ServerWebExchange securedExchange = MockServerWebExchange.from(securedRequest);

        capturedExchange.set(null);
        authenticationFilter.apply(new AuthenticationFilter.Config()).filter(securedExchange, chain).block();

        assertNotNull(capturedExchange.get());
        assertEquals("42", capturedExchange.get().getRequest().getHeaders().getFirst("X-User-Id"),
                "Spoofed X-User-Id must be overwritten with authenticated token claim");
        assertEquals("ROLE_BIDDER", capturedExchange.get().getRequest().getHeaders().getFirst("X-User-Role"),
                "Spoofed X-User-Role must be overwritten with authenticated token claim");
    }
}

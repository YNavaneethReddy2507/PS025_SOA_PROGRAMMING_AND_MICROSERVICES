package com.auction.auth.security;

import com.auction.auth.entity.Role;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class JwtSecurityTest {

    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 3600000L; // 1 hour

    private JwtTokenProvider jwtTokenProvider;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(TEST_SECRET, EXPIRATION_MS);
        passwordEncoder = new BCryptPasswordEncoder();
    }

    @Test
    @DisplayName("Test token generation includes userId, username, role, and subject claims")
    void testTokenGenerationAndClaims() {
        String token = jwtTokenProvider.generateToken(42L, "user@auction.com", "auctioneer", Role.SELLER);

        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3, "JWT must have 3 segments separated by dots");

        Claims claims = jwtTokenProvider.extractAllClaims(token);
        assertEquals("user@auction.com", claims.getSubject());
        assertEquals(42L, claims.get("userId", Long.class));
        assertEquals("auctioneer", claims.get("username", String.class));
        assertEquals("SELLER", claims.get("role", String.class));

        assertEquals("user@auction.com", jwtTokenProvider.extractEmail(token));
        assertEquals(42L, jwtTokenProvider.extractUserId(token));
        assertEquals("auctioneer", jwtTokenProvider.extractUsername(token));
        assertEquals(Role.SELLER, jwtTokenProvider.extractRole(token));
    }

    @Test
    @DisplayName("Test token validation accepts freshly issued valid token")
    void testValidateValidToken() {
        String token = jwtTokenProvider.generateToken(1L, "valid@test.com", "validuser", Role.BUYER);
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("Test token validation strictly rejects expired tokens")
    void testRejectExpiredToken() {
        // Generate token with negative expiration (-5 seconds in the past)
        String expiredToken = jwtTokenProvider.generateTokenWithExpiry(1L, "expired@test.com", "expireduser", Role.BUYER, -5000L);

        assertFalse(jwtTokenProvider.validateToken(expiredToken), "Expired token must be rejected");
    }

    @Test
    @DisplayName("Test token validation strictly rejects malformed tokens")
    void testRejectMalformedToken() {
        assertFalse(jwtTokenProvider.validateToken("not.a.valid.jwt.token"));
        assertFalse(jwtTokenProvider.validateToken("header.payload"));
        assertFalse(jwtTokenProvider.validateToken("header.payload.signature.extra"));
        assertFalse(jwtTokenProvider.validateToken("randomgibberish12345"));
    }

    @Test
    @DisplayName("Test token validation strictly rejects missing, empty, or blank tokens")
    void testRejectMissingTokens() {
        assertFalse(jwtTokenProvider.validateToken(null));
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken("   "));
    }

    @Test
    @DisplayName("Test token validation rejects token signed with an untrusted secret key")
    void testRejectInvalidSignature() {
        String foreignSecret = "586E3272357538782F413F4428472B4B6250645367566B5970404E635266556A";
        JwtTokenProvider untrustedProvider = new JwtTokenProvider(foreignSecret, EXPIRATION_MS);

        String rogueToken = untrustedProvider.generateToken(99L, "rogue@hack.com", "rogue", Role.ADMIN);

        assertFalse(jwtTokenProvider.validateToken(rogueToken), "Token signed with wrong key must be rejected");
    }

    @Test
    @DisplayName("Test BCrypt password hashing produces secure hashes and never stores plaintext")
    void testBCryptPasswordHashing() {
        String rawPassword = "SuperSecurePassword#2026";
        String encodedHash = passwordEncoder.encode(rawPassword);

        assertNotNull(encodedHash);
        assertNotEquals(rawPassword, encodedHash);
        assertTrue(encodedHash.startsWith("$2a$") || encodedHash.startsWith("$2b$"));
        assertTrue(passwordEncoder.matches(rawPassword, encodedHash));
        assertFalse(passwordEncoder.matches("WrongPassword", encodedHash));
    }

    @Test
    @DisplayName("Test ReusableJwtSecurityConfig StandaloneJwtValidator reusable component")
    void testStandaloneJwtValidatorReusable() {
        ReusableJwtSecurityConfig.StandaloneJwtValidator validator =
                new ReusableJwtSecurityConfig.StandaloneJwtValidator(TEST_SECRET);

        String validToken = jwtTokenProvider.generateToken(10L, "buyer@test.com", "buyer10", Role.BUYER);
        assertTrue(validator.isValid(validToken));
        assertEquals("buyer@test.com", validator.getClaims(validToken).getSubject());

        assertFalse(validator.isValid("malformed.token.value"));
        assertFalse(validator.isValid(null));
    }
}

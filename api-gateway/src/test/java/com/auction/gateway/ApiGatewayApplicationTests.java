package com.auction.gateway;

import com.auction.gateway.filter.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ApiGatewayApplicationTests {

    private final JwtUtil jwtUtil = new JwtUtil("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");

    @Test
    void contextLoads() {
    }

    @Test
    void testJwtValidationWithInvalidToken() {
        boolean valid = jwtUtil.validateToken("invalid.jwt.token");
        assertFalse(valid);
    }
}

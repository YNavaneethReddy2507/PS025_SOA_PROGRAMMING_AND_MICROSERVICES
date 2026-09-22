package com.auction.bidding.security;

import com.auction.bidding.dto.BidResponse;
import com.auction.bidding.dto.PlaceBidRequest;
import com.auction.bidding.entity.BidStatus;
import com.auction.bidding.service.BiddingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BiddingSecurityTest {

    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BiddingService biddingService;

    private PlaceBidRequest validBidRequest;

    @BeforeEach
    void setUp() {
        validBidRequest = PlaceBidRequest.builder()
                .auctionId(1L)
                .bidAmount(new BigDecimal("150.00"))
                .build();
    }

    private String generateToken(Long userId, String email, String role, long expiryMs) {
        byte[] keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(SECRET);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);

        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(key)
                .compact();
    }

    @Test
    @DisplayName("Bid with valid JWT token extracts bidder identity correctly")
    void testBidWithValidTokenSucceeds() throws Exception {
        String token = generateToken(55L, "validbidder@example.com", "ROLE_BIDDER", 3600000);

        BidResponse response = BidResponse.builder()
                .id(1L)
                .auctionId(1L)
                .bidderId(55L)
                .amount(new BigDecimal("150.00"))
                .status(BidStatus.ACCEPTED)
                .acceptedAt(LocalDateTime.now())
                .message("Bid placed")
                .isWinning(true)
                .build();

        when(biddingService.placeBid(any(PlaceBidRequest.class), eq(55L))).thenReturn(response);

        mockMvc.perform(post("/api/bids")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBidRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bidderId").value(55L))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    @DisplayName("Bid with expired JWT token is rejected with 403")
    void testBidWithExpiredTokenFails() throws Exception {
        // Expired 1 hour ago
        String expiredToken = generateToken(55L, "bidder@example.com", "ROLE_BIDDER", -3600000);

        mockMvc.perform(post("/api/bids")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBidRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Bid with malformed/tampered JWT token is rejected with 403")
    void testBidWithMalformedTokenFails() throws Exception {
        String malformedToken = "eyJhbGciOiJIUzI1NiJ9.invalidpayload.invalidsignature";

        mockMvc.perform(post("/api/bids")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + malformedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBidRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Bid without any authentication is rejected with 403")
    void testBidWithoutAuthenticationFails() throws Exception {
        mockMvc.perform(post("/api/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBidRequest)))
                .andExpect(status().isForbidden());
    }
}

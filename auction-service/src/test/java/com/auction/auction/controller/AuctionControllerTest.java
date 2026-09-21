package com.auction.auction.controller;

import com.auction.auction.dto.AuctionResponse;
import com.auction.auction.dto.CreateAuctionRequest;
import com.auction.auction.dto.UpdateAuctionRequest;
import com.auction.auction.entity.AuctionStatus;
import com.auction.auction.security.JwtUtil;
import com.auction.auction.service.AuctionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuctionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuctionService auctionService;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("POST /api/auctions creates auction with seller identity from header")
    void testCreateAuctionWithHeader() throws Exception {
        LocalDateTime now = LocalDateTime.now().plusHours(1);
        CreateAuctionRequest request = CreateAuctionRequest.builder()
                .title("Antique Vase")
                .description("Handmade porcelain")
                .category("Art")
                .startingPrice(new BigDecimal("50.00"))
                .minimumIncrement(new BigDecimal("5.00"))
                .startTime(now)
                .endTime(now.plusDays(2))
                .build();

        AuctionResponse response = AuctionResponse.builder()
                .id(1L)
                .title("Antique Vase")
                .category("Art")
                .startingPrice(new BigDecimal("50.00"))
                .currentPrice(new BigDecimal("50.00"))
                .minimumIncrement(new BigDecimal("5.00"))
                .sellerId(1L)
                .status(AuctionStatus.SCHEDULED)
                .startTime(now)
                .endTime(now.plusDays(2))
                .build();

        when(auctionService.createAuction(any(CreateAuctionRequest.class), eq(1L))).thenReturn(response);

        mockMvc.perform(post("/api/auctions")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Antique Vase"))
                .andExpect(jsonPath("$.startingPrice").value(50.00))
                .andExpect(jsonPath("$.currentPrice").value(50.00))
                .andExpect(jsonPath("$.minimumIncrement").value(5.00));
    }

    @Test
    @DisplayName("POST /api/auctions fails with 400 when startingPrice is 0 or negative")
    void testCreateAuctionValidationFailure() throws Exception {
        LocalDateTime now = LocalDateTime.now().plusHours(1);
        CreateAuctionRequest request = CreateAuctionRequest.builder()
                .title("Invalid Auction")
                .startingPrice(BigDecimal.ZERO)
                .minimumIncrement(new BigDecimal("5.00"))
                .startTime(now)
                .endTime(now.plusDays(2))
                .build();

        mockMvc.perform(post("/api/auctions")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auctions fails with 403 when authentication header is missing")
    void testCreateAuctionMissingAuth() throws Exception {
        LocalDateTime now = LocalDateTime.now().plusHours(1);
        CreateAuctionRequest request = CreateAuctionRequest.builder()
                .title("Unauthenticated Auction")
                .startingPrice(new BigDecimal("50.00"))
                .minimumIncrement(new BigDecimal("5.00"))
                .startTime(now)
                .endTime(now.plusDays(2))
                .build();

        mockMvc.perform(post("/api/auctions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/auctions/{id} returns auction details")
    void testGetAuctionById() throws Exception {
        AuctionResponse response = AuctionResponse.builder()
                .id(1L)
                .title("Antique Vase")
                .currentPrice(new BigDecimal("50.00"))
                .minimumIncrement(new BigDecimal("5.00"))
                .status(AuctionStatus.ACTIVE)
                .build();

        when(auctionService.getAuctionById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/auctions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Antique Vase"));
    }

    @Test
    @DisplayName("GET /api/auctions returns all auctions")
    void testGetAllAuctions() throws Exception {
        AuctionResponse response = AuctionResponse.builder()
                .id(1L)
                .title("Antique Vase")
                .status(AuctionStatus.ACTIVE)
                .build();

        when(auctionService.getAuctions(null, null)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/auctions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("PUT /api/auctions/{id} updates auction successfully")
    void testUpdateAuction() throws Exception {
        UpdateAuctionRequest updateReq = UpdateAuctionRequest.builder()
                .title("Updated Vase Title")
                .minimumIncrement(new BigDecimal("10.00"))
                .build();

        AuctionResponse response = AuctionResponse.builder()
                .id(1L)
                .title("Updated Vase Title")
                .minimumIncrement(new BigDecimal("10.00"))
                .status(AuctionStatus.SCHEDULED)
                .build();

        when(auctionService.updateAuction(eq(1L), any(UpdateAuctionRequest.class), eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/auctions/1")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Vase Title"));
    }

    @Test
    @DisplayName("DELETE /api/auctions/{id} deletes auction successfully")
    void testDeleteAuction() throws Exception {
        doNothing().when(auctionService).deleteAuction(eq(1L), eq(1L), any());

        mockMvc.perform(delete("/api/auctions/1")
                        .header("X-User-Id", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/auctions/{id}/start starts scheduled auction")
    void testStartAuction() throws Exception {
        AuctionResponse response = AuctionResponse.builder()
                .id(1L)
                .status(AuctionStatus.ACTIVE)
                .build();

        when(auctionService.startAuction(eq(1L), eq(1L), any())).thenReturn(response);

        mockMvc.perform(post("/api/auctions/1/start")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/auctions/{id}/close closes active auction")
    void testCloseAuction() throws Exception {
        AuctionResponse response = AuctionResponse.builder()
                .id(1L)
                .status(AuctionStatus.CLOSED)
                .build();

        when(auctionService.closeAuction(eq(1L), eq(1L), any())).thenReturn(response);

        mockMvc.perform(post("/api/auctions/1/close")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    @DisplayName("POST /api/auctions/{id}/cancel cancels auction")
    void testCancelAuction() throws Exception {
        AuctionResponse response = AuctionResponse.builder()
                .id(1L)
                .status(AuctionStatus.CANCELLED)
                .build();

        when(auctionService.cancelAuction(eq(1L), eq(1L), any())).thenReturn(response);

        mockMvc.perform(post("/api/auctions/1/cancel")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}

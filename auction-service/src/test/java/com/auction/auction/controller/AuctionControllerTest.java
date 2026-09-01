package com.auction.auction.controller;

import com.auction.auction.dto.AuctionResponse;
import com.auction.auction.dto.CreateAuctionRequest;
import com.auction.auction.entity.AuctionStatus;
import com.auction.auction.service.AuctionService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    void testCreateAuction() throws Exception {
        LocalDateTime now = LocalDateTime.now().plusHours(1);
        CreateAuctionRequest request = CreateAuctionRequest.builder()
                .title("Antique Vase")
                .description("Handmade porcelain")
                .category("Art")
                .startingPrice(new BigDecimal("50.00"))
                .reservePrice(new BigDecimal("150.00"))
                .minBidIncrement(new BigDecimal("5.00"))
                .startTime(now)
                .endTime(now.plusDays(2))
                .build();

        AuctionResponse response = AuctionResponse.builder()
                .id(1L)
                .title("Antique Vase")
                .category("Art")
                .startingPrice(new BigDecimal("50.00"))
                .reservePrice(new BigDecimal("150.00"))
                .minBidIncrement(new BigDecimal("5.00"))
                .currentHighestBid(new BigDecimal("50.00"))
                .sellerId(1L)
                .status(AuctionStatus.ACTIVE)
                .startTime(now)
                .endTime(now.plusDays(2))
                .build();

        when(auctionService.createAuction(any(CreateAuctionRequest.class), eq(1L))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auctions")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Antique Vase"));
    }

    @Test
    void testGetAuctionById() throws Exception {
        AuctionResponse response = AuctionResponse.builder()
                .id(1L)
                .title("Antique Vase")
                .status(AuctionStatus.ACTIVE)
                .build();

        when(auctionService.getAuctionById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/auctions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Antique Vase"));
    }

    @Test
    void testGetAllAuctions() throws Exception {
        AuctionResponse response = AuctionResponse.builder()
                .id(1L)
                .title("Antique Vase")
                .status(AuctionStatus.ACTIVE)
                .build();

        when(auctionService.getAuctions(null, null)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/auctions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}

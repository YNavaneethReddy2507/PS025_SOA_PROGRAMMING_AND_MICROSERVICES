package com.auction.bidding.controller;

import com.auction.bidding.dto.BidResponse;
import com.auction.bidding.dto.ClearanceResultDto;
import com.auction.bidding.dto.PlaceBidRequest;
import com.auction.bidding.entity.BidStatus;
import com.auction.bidding.security.JwtUtil;
import com.auction.bidding.service.BiddingService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BiddingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BiddingService biddingService;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("POST /api/bids places bid successfully with authenticated bidder")
    void testPlaceBidEndpointSuccess() throws Exception {
        PlaceBidRequest request = PlaceBidRequest.builder()
                .auctionId(1L)
                .bidAmount(new BigDecimal("150.00"))
                .build();

        BidResponse response = BidResponse.builder()
                .id(10L)
                .auctionId(1L)
                .bidderId(2L)
                .amount(new BigDecimal("150.00"))
                .status(BidStatus.ACCEPTED)
                .acceptedAt(LocalDateTime.now())
                .message("Bid accepted successfully")
                .isWinning(true)
                .build();

        when(biddingService.placeBid(any(PlaceBidRequest.class), eq(2L))).thenReturn(response);

        mockMvc.perform(post("/api/bids")
                        .header("X-User-Id", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.amount").value(150.00))
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.bidderId").value(2L));
    }

    @Test
    @DisplayName("POST /api/bids rejects unauthenticated bid requests")
    void testPlaceBidUnauthenticated() throws Exception {
        PlaceBidRequest request = PlaceBidRequest.builder()
                .auctionId(1L)
                .bidAmount(new BigDecimal("150.00"))
                .build();

        mockMvc.perform(post("/api/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/bids/{id} retrieves bid by ID")
    void testGetBidById() throws Exception {
        BidResponse response = BidResponse.builder()
                .id(10L)
                .auctionId(1L)
                .bidderId(2L)
                .amount(new BigDecimal("150.00"))
                .status(BidStatus.ACCEPTED)
                .acceptedAt(LocalDateTime.now())
                .build();

        when(biddingService.getBidById(10L)).thenReturn(response);

        mockMvc.perform(get("/api/bids/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.amount").value(150.00));
    }

    @Test
    @DisplayName("GET /api/auctions/{auctionId}/bids retrieves all bids for an auction")
    void testGetBidsByAuction() throws Exception {
        BidResponse response = BidResponse.builder()
                .id(10L)
                .auctionId(1L)
                .bidderId(2L)
                .amount(new BigDecimal("150.00"))
                .status(BidStatus.ACCEPTED)
                .acceptedAt(LocalDateTime.now())
                .build();

        when(biddingService.getBidsByAuction(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/auctions/1/bids"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/auctions/{auctionId}/highest retrieves current highest bid")
    void testGetHighestBid() throws Exception {
        BidResponse response = BidResponse.builder()
                .id(10L)
                .auctionId(1L)
                .bidderId(2L)
                .amount(new BigDecimal("200.00"))
                .status(BidStatus.ACCEPTED)
                .acceptedAt(LocalDateTime.now())
                .build();

        when(biddingService.getHighestBid(1L)).thenReturn(response);

        mockMvc.perform(get("/api/auctions/1/highest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.amount").value(200.00));
    }

    @Test
    @DisplayName("GET /api/v1/bids/auction/1/clearance calculates clearance")
    void testCalculateClearanceEndpoint() throws Exception {
        ClearanceResultDto result = ClearanceResultDto.builder()
                .auctionId(1L)
                .winningBidderId(2L)
                .winningAmount(new BigDecimal("150.00"))
                .reserveMet(true)
                .clearanceStatus("CLEARED")
                .build();

        when(biddingService.calculateClearance(1L)).thenReturn(result);

        mockMvc.perform(get("/api/bids/auction/1/clearance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auctionId").value(1L))
                .andExpect(jsonPath("$.winningBidderId").value(2L))
                .andExpect(jsonPath("$.clearanceStatus").value("CLEARED"));
    }
}

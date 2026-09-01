package com.auction.bidding.controller;

import com.auction.bidding.dto.BidResponse;
import com.auction.bidding.dto.ClearanceResultDto;
import com.auction.bidding.dto.PlaceBidRequest;
import com.auction.bidding.entity.BidStatus;
import com.auction.bidding.service.BiddingService;
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
class BiddingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BiddingService biddingService;

    @Test
    void testPlaceBidEndpointSuccess() throws Exception {
        PlaceBidRequest request = PlaceBidRequest.builder()
                .auctionId(1L)
                .bidAmount(new BigDecimal("150.00"))
                .build();

        BidResponse response = BidResponse.builder()
                .id(10L)
                .auctionId(1L)
                .bidderId(2L)
                .bidAmount(new BigDecimal("150.00"))
                .status(BidStatus.WINNING)
                .bidTimestamp(LocalDateTime.now())
                .isWinning(true)
                .build();

        when(biddingService.placeBid(any(PlaceBidRequest.class), eq(2L))).thenReturn(response);

        mockMvc.perform(post("/api/v1/bids")
                        .header("X-User-Id", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.bidAmount").value(150.00))
                .andExpect(jsonPath("$.winning").value(true));
    }

    @Test
    void testGetBidsByAuction() throws Exception {
        BidResponse response = BidResponse.builder()
                .id(10L)
                .auctionId(1L)
                .bidderId(2L)
                .bidAmount(new BigDecimal("150.00"))
                .status(BidStatus.WINNING)
                .bidTimestamp(LocalDateTime.now())
                .isWinning(true)
                .build();

        when(biddingService.getBidsByAuction(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/bids/auction/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void testCalculateClearanceEndpoint() throws Exception {
        ClearanceResultDto result = ClearanceResultDto.builder()
                .auctionId(1L)
                .winningBidderId(2L)
                .winningAmount(new BigDecimal("150.00"))
                .reserveMet(true)
                .clearanceStatus("CLEARED")
                .build();

        when(biddingService.calculateClearance(1L)).thenReturn(result);

        mockMvc.perform(get("/api/v1/bids/auction/1/clearance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auctionId").value(1L))
                .andExpect(jsonPath("$.winningBidderId").value(2L))
                .andExpect(jsonPath("$.clearanceStatus").value("CLEARED"));
    }
}

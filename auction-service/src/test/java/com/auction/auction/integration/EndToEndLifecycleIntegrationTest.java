package com.auction.auction.integration;

import com.auction.auction.client.BiddingClient;
import com.auction.auction.client.PaymentClient;
import com.auction.auction.dto.ClearanceResultDto;
import com.auction.auction.dto.CreateAuctionRequest;
import com.auction.auction.dto.PaymentSettlementRequest;
import com.auction.auction.dto.PaymentSettlementResponse;
import com.auction.auction.dto.UpdateHighestBidRequest;
import com.auction.auction.entity.AuctionStatus;
import com.auction.auction.repository.AuctionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EndToEndLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuctionRepository auctionRepository;

    @MockBean
    private BiddingClient biddingClient;

    @MockBean
    private PaymentClient paymentClient;

    @BeforeEach
    void setUp() {
        auctionRepository.deleteAll();
    }

    @Test
    @DisplayName("E2E Lifecycle: Create -> Start -> Bid 1 -> Bid 2 (Higher) -> Close -> Clearance Winner -> Settle Payment")
    void testCompleteAuctionLifecycle() throws Exception {
        Long sellerId = 10L;
        Long bidder1Id = 20L;
        Long bidder2Id = 30L;

        // 1. Create Auction: Seller creates an auction scheduled for future
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        LocalDateTime endTime = LocalDateTime.now().plusDays(3);

        CreateAuctionRequest createRequest = CreateAuctionRequest.builder()
                .title("Vintage 1968 Omega Speedmaster")
                .description("Authentic vintage chronometer in mint condition")
                .category("Watches")
                .startingPrice(new BigDecimal("100.00"))
                .minimumIncrement(new BigDecimal("10.00"))
                .reservePrice(new BigDecimal("150.00"))
                .startTime(startTime)
                .endTime(endTime)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/auctions")
                        .header("X-User-Id", sellerId)
                        .header("X-User-Role", "ROLE_SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.startingPrice").value(100.00))
                .andExpect(jsonPath("$.sellerId").value(sellerId))
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        Long auctionId = objectMapper.readTree(responseBody).get("id").asLong();

        // 2. Start Auction: Seller activates the auction early
        mockMvc.perform(post("/api/auctions/" + auctionId + "/start")
                        .header("X-User-Id", sellerId)
                        .header("X-User-Role", "ROLE_SELLER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // 3. Place Bid 1: Bidder 1 bids $120.00
        UpdateHighestBidRequest bid1 = UpdateHighestBidRequest.builder()
                .bidderId(bidder1Id)
                .amount(new BigDecimal("120.00"))
                .build();

        mockMvc.perform(put("/api/auctions/" + auctionId + "/highest-bid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bid1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPrice").value(120.00))
                .andExpect(jsonPath("$.winnerId").value(bidder1Id));

        // 4. Place Higher Bid: Bidder 2 bids $160.00 (meets reserve price of $150.00)
        UpdateHighestBidRequest bid2 = UpdateHighestBidRequest.builder()
                .bidderId(bidder2Id)
                .amount(new BigDecimal("160.00"))
                .build();

        mockMvc.perform(put("/api/auctions/" + auctionId + "/highest-bid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bid2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPrice").value(160.00))
                .andExpect(jsonPath("$.winnerId").value(bidder2Id));

        // 5. Setup Mock Bidding Clearance and Payment Settlement
        ClearanceResultDto clearanceResult = ClearanceResultDto.builder()
                .auctionId(auctionId)
                .winningBidderId(bidder2Id)
                .winningAmount(new BigDecimal("160.00"))
                .reserveMet(true)
                .clearedAt(LocalDateTime.now())
                .clearanceStatus("CLEARED")
                .message("Deterministic winner selected")
                .build();

        PaymentSettlementResponse settlementResponse = PaymentSettlementResponse.builder()
                .transactionId("tx-e2e-" + auctionId)
                .auctionId(auctionId)
                .buyerId(bidder2Id)
                .sellerId(sellerId)
                .amount(new BigDecimal("160.00"))
                .platformFee(new BigDecimal("8.00"))
                .netSellerPayout(new BigDecimal("152.00"))
                .status("COMPLETED")
                .settledAt(LocalDateTime.now())
                .build();

        when(biddingClient.calculateClearance(eq(auctionId))).thenReturn(clearanceResult);
        when(paymentClient.processSettlement(any(PaymentSettlementRequest.class))).thenReturn(settlementResponse);

        // 6. Close Auction: Seller triggers auction closure and clearance
        mockMvc.perform(post("/api/auctions/" + auctionId + "/clear"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.winningBidderId").value(bidder2Id))
                .andExpect(jsonPath("$.winningAmount").value(160.00))
                .andExpect(jsonPath("$.reserveMet").value(true))
                .andExpect(jsonPath("$.clearanceStatus").value("CLEARED"));

        // 7. Post-Close Negative Check: Cannot place a new bid on a CLOSED auction (HTTP 409 Conflict)
        UpdateHighestBidRequest postCloseBid = UpdateHighestBidRequest.builder()
                .bidderId(999L)
                .amount(new BigDecimal("200.00"))
                .build();

        mockMvc.perform(put("/api/auctions/" + auctionId + "/highest-bid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postCloseBid)))
                .andExpect(status().isConflict());

        // 8. Post-Close Negative Check: Cannot cancel a CLOSED auction
        mockMvc.perform(post("/api/auctions/" + auctionId + "/cancel")
                        .header("X-User-Id", sellerId)
                        .header("X-User-Role", "ROLE_SELLER"))
                .andExpect(status().isConflict());
    }
}

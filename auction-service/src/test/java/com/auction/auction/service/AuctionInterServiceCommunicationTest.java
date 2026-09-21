package com.auction.auction.service;

import com.auction.auction.client.BiddingClient;
import com.auction.auction.client.PaymentClient;
import com.auction.auction.dto.ClearanceResultDto;
import com.auction.auction.dto.PaymentSettlementRequest;
import com.auction.auction.dto.PaymentSettlementResponse;
import com.auction.auction.entity.Auction;
import com.auction.auction.entity.AuctionStatus;
import com.auction.auction.exception.ServiceUnavailableException;
import com.auction.auction.repository.AuctionRepository;
import com.auction.auction.service.impl.AuctionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionInterServiceCommunicationTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private BiddingClient biddingClient;

    @Mock
    private PaymentClient paymentClient;

    @InjectMocks
    private AuctionServiceImpl auctionService;

    private Auction testAuction;

    @BeforeEach
    void setUp() {
        testAuction = Auction.builder()
                .id(100L)
                .title("MacBook Pro M3")
                .description("Latest M3 Max chip")
                .category("Electronics")
                .startingPrice(new BigDecimal("1000.00"))
                .currentPrice(new BigDecimal("1000.00"))
                .minimumIncrement(new BigDecimal("50.00"))
                .reservePrice(new BigDecimal("1500.00"))
                .sellerId(10L)
                .status(AuctionStatus.ACTIVE)
                .startTime(LocalDateTime.now().minusHours(2))
                .endTime(LocalDateTime.now().plusHours(2))
                .build();
    }

    @Test
    @DisplayName("Verify auction closure initiates payment workflow across services")
    void testAuctionClosureInitiatesPaymentWorkflow() {
        when(auctionRepository.findById(100L)).thenReturn(Optional.of(testAuction));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        ClearanceResultDto clearanceResult = ClearanceResultDto.builder()
                .auctionId(100L)
                .winningBidderId(55L)
                .winningAmount(new BigDecimal("1800.00"))
                .reserveMet(true)
                .clearedAt(LocalDateTime.now())
                .clearanceStatus("CLEARED")
                .message("Deterministic winner selected")
                .build();

        when(biddingClient.calculateClearance(100L)).thenReturn(clearanceResult);

        PaymentSettlementResponse settlementResponse = PaymentSettlementResponse.builder()
                .transactionId("tx-settle-999")
                .auctionId(100L)
                .buyerId(55L)
                .sellerId(10L)
                .amount(new BigDecimal("1800.00"))
                .platformFee(new BigDecimal("90.00"))
                .netSellerPayout(new BigDecimal("1710.00"))
                .status("COMPLETED")
                .settledAt(LocalDateTime.now())
                .build();

        when(paymentClient.processSettlement(any(PaymentSettlementRequest.class))).thenReturn(settlementResponse);

        ClearanceResultDto result = auctionService.closeAndClearAuction(100L);

        assertNotNull(result);
        assertEquals(55L, result.getWinningBidderId());
        assertEquals(0, new BigDecimal("1800.00").compareTo(result.getWinningAmount()));
        assertTrue(result.isReserveMet());

        ArgumentCaptor<PaymentSettlementRequest> captor = ArgumentCaptor.forClass(PaymentSettlementRequest.class);
        verify(paymentClient, times(1)).processSettlement(captor.capture());

        PaymentSettlementRequest capturedReq = captor.getValue();
        assertEquals(100L, capturedReq.getAuctionId());
        assertEquals(10L, capturedReq.getSellerId());
        assertEquals(55L, capturedReq.getBuyerId());
        assertEquals(0, new BigDecimal("1800.00").compareTo(capturedReq.getAmount()));

        assertEquals(AuctionStatus.CLOSED, testAuction.getStatus());
        assertEquals(55L, testAuction.getWinnerId());
        assertEquals(0, new BigDecimal("1800.00").compareTo(testAuction.getCurrentPrice()));
    }

    @Test
    @DisplayName("Verify downstream payment failure is handled safely without corrupting auction state")
    void testAuctionClosureHandlesDownstreamPaymentFailureGracefully() {
        when(auctionRepository.findById(100L)).thenReturn(Optional.of(testAuction));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        ClearanceResultDto clearanceResult = ClearanceResultDto.builder()
                .auctionId(100L)
                .winningBidderId(55L)
                .winningAmount(new BigDecimal("1800.00"))
                .reserveMet(true)
                .clearedAt(LocalDateTime.now())
                .clearanceStatus("CLEARED")
                .build();

        when(biddingClient.calculateClearance(100L)).thenReturn(clearanceResult);
        when(paymentClient.processSettlement(any(PaymentSettlementRequest.class)))
                .thenThrow(new ServiceUnavailableException("Payment gateway unreachable"));

        // Should not throw exception, handles gracefully
        assertDoesNotThrow(() -> auctionService.closeAndClearAuction(100L));

        assertEquals(AuctionStatus.CLOSED, testAuction.getStatus());
        assertEquals(55L, testAuction.getWinnerId());
    }

    @Test
    @DisplayName("Verify bidding service failure triggers local clearance fallback safely")
    void testAuctionClosureHandlesBiddingServiceFailureGracefully() {
        testAuction.setWinnerId(88L);
        testAuction.setCurrentPrice(new BigDecimal("1600.00"));

        when(auctionRepository.findById(100L)).thenReturn(Optional.of(testAuction));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(biddingClient.calculateClearance(100L))
                .thenThrow(new ServiceUnavailableException("Bidding service timed out"));

        ClearanceResultDto result = auctionService.closeAndClearAuction(100L);

        assertNotNull(result);
        assertEquals(88L, result.getWinningBidderId());
        assertEquals(AuctionStatus.CLOSED, testAuction.getStatus());
    }
}

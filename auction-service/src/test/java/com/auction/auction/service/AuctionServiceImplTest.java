package com.auction.auction.service;

import com.auction.auction.client.BiddingClient;
import com.auction.auction.client.PaymentClient;
import com.auction.auction.dto.AuctionResponse;
import com.auction.auction.dto.ClearanceResultDto;
import com.auction.auction.dto.CreateAuctionRequest;
import com.auction.auction.dto.PaymentSettlementResponse;
import com.auction.auction.entity.Auction;
import com.auction.auction.entity.AuctionStatus;
import com.auction.auction.exception.InvalidAuctionStateException;
import com.auction.auction.exception.ResourceNotFoundException;
import com.auction.auction.exception.UnauthorizedActionException;
import com.auction.auction.repository.AuctionRepository;
import com.auction.auction.service.impl.AuctionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class AuctionServiceImplTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private BiddingClient biddingClient;

    @Mock
    private PaymentClient paymentClient;

    @InjectMocks
    private AuctionServiceImpl auctionService;

    private Auction sampleAuction;
    private CreateAuctionRequest createRequest;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        sampleAuction = Auction.builder()
                .id(1L)
                .title("Vintage Watch")
                .description("Rare 1960s watch")
                .category("Collectibles")
                .startingPrice(new BigDecimal("100.00"))
                .reservePrice(new BigDecimal("200.00"))
                .minBidIncrement(new BigDecimal("10.00"))
                .currentHighestBid(new BigDecimal("100.00"))
                .sellerId(10L)
                .status(AuctionStatus.ACTIVE)
                .startTime(now)
                .endTime(now.plusDays(3))
                .build();

        createRequest = CreateAuctionRequest.builder()
                .title("Vintage Watch")
                .description("Rare 1960s watch")
                .category("Collectibles")
                .startingPrice(new BigDecimal("100.00"))
                .reservePrice(new BigDecimal("200.00"))
                .minBidIncrement(new BigDecimal("10.00"))
                .startTime(now)
                .endTime(now.plusDays(3))
                .build();
    }

    @Test
    void testCreateAuctionSuccess() {
        when(auctionRepository.save(any(Auction.class))).thenReturn(sampleAuction);

        AuctionResponse response = auctionService.createAuction(createRequest, 10L);

        assertNotNull(response);
        assertEquals("Vintage Watch", response.getTitle());
        assertEquals(new BigDecimal("100.00"), response.getStartingPrice());
        assertEquals(10L, response.getSellerId());
        verify(auctionRepository, times(1)).save(any(Auction.class));
    }

    @Test
    void testGetAuctionByIdSuccess() {
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(sampleAuction));

        AuctionResponse response = auctionService.getAuctionById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Vintage Watch", response.getTitle());
    }

    @Test
    void testGetAuctionByIdNotFound() {
        when(auctionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> auctionService.getAuctionById(999L));
    }

    @Test
    void testCancelAuctionSuccessBySeller() {
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(sampleAuction));
        when(auctionRepository.save(any(Auction.class))).thenReturn(sampleAuction);

        AuctionResponse response = auctionService.cancelAuction(1L, 10L, "ROLE_SELLER");

        assertNotNull(response);
        verify(auctionRepository, times(1)).save(any(Auction.class));
    }

    @Test
    void testCancelAuctionForbiddenForOtherUser() {
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(sampleAuction));

        assertThrows(UnauthorizedActionException.class, () -> auctionService.cancelAuction(1L, 99L, "ROLE_SELLER"));
    }

    @Test
    void testCloseAndClearAuctionSettled() {
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(sampleAuction));
        when(auctionRepository.save(any(Auction.class))).thenReturn(sampleAuction);

        ClearanceResultDto clearanceResult = ClearanceResultDto.builder()
                .auctionId(1L)
                .winningBidderId(42L)
                .winningAmount(new BigDecimal("250.00"))
                .reserveMet(true)
                .clearedAt(LocalDateTime.now())
                .clearanceStatus("CLEARED")
                .build();

        when(biddingClient.calculateClearance(1L)).thenReturn(clearanceResult);

        PaymentSettlementResponse paymentResponse = PaymentSettlementResponse.builder()
                .transactionId("tx-12345")
                .auctionId(1L)
                .status("COMPLETED")
                .build();

        when(paymentClient.processSettlement(any())).thenReturn(paymentResponse);

        ClearanceResultDto result = auctionService.closeAndClearAuction(1L);

        assertNotNull(result);
        assertEquals(42L, result.getWinningBidderId());
        assertTrue(result.isReserveMet());
        verify(paymentClient, times(1)).processSettlement(any());
    }
}

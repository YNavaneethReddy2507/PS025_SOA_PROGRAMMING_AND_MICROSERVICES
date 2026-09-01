package com.auction.bidding.service;

import com.auction.bidding.client.AuctionClient;
import com.auction.bidding.dto.BidResponse;
import com.auction.bidding.dto.ClearanceResultDto;
import com.auction.bidding.dto.PlaceBidRequest;
import com.auction.bidding.entity.AuctionBidState;
import com.auction.bidding.entity.Bid;
import com.auction.bidding.entity.BidStatus;
import com.auction.bidding.exception.SellerCannotBidException;
import com.auction.bidding.exception.SubThresholdBidException;
import com.auction.bidding.repository.AuctionBidStateRepository;
import com.auction.bidding.repository.BidRepository;
import com.auction.bidding.service.impl.BiddingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BiddingServiceImplTest {

    @Mock
    private BidRepository bidRepository;

    @Mock
    private AuctionBidStateRepository stateRepository;

    @Mock
    private AuctionClient auctionClient;

    @InjectMocks
    private BiddingServiceImpl biddingService;

    private AuctionBidState sampleState;

    @BeforeEach
    void setUp() {
        sampleState = AuctionBidState.builder()
                .auctionId(1L)
                .currentHighestBid(new BigDecimal("100.00"))
                .winningBidderId(20L)
                .minBidIncrement(new BigDecimal("10.00"))
                .reservePrice(new BigDecimal("200.00"))
                .sellerId(10L)
                .totalBidsCount(1L)
                .build();
    }

    @Test
    void testPlaceBidSuccess() {
        when(stateRepository.findByAuctionId(1L)).thenReturn(Optional.of(sampleState));

        Bid savedBid = Bid.builder()
                .id(101L)
                .auctionId(1L)
                .bidderId(30L)
                .bidAmount(new BigDecimal("120.00"))
                .status(BidStatus.WINNING)
                .bidTimestamp(LocalDateTime.now())
                .build();

        when(bidRepository.save(any(Bid.class))).thenReturn(savedBid);
        when(stateRepository.save(any(AuctionBidState.class))).thenReturn(sampleState);

        PlaceBidRequest request = PlaceBidRequest.builder()
                .auctionId(1L)
                .bidAmount(new BigDecimal("120.00"))
                .build();

        BidResponse response = biddingService.placeBid(request, 30L);

        assertNotNull(response);
        assertEquals(new BigDecimal("120.00"), response.getBidAmount());
        assertEquals(30L, response.getBidderId());
        assertTrue(response.isWinning());
        verify(bidRepository, times(1)).save(any(Bid.class));
    }

    @Test
    void testPlaceBidSellerCannotBidThrowsException() {
        when(stateRepository.findByAuctionId(1L)).thenReturn(Optional.of(sampleState));

        PlaceBidRequest request = PlaceBidRequest.builder()
                .auctionId(1L)
                .bidAmount(new BigDecimal("150.00"))
                .build();

        assertThrows(SellerCannotBidException.class, () -> biddingService.placeBid(request, 10L));
    }

    @Test
    void testPlaceBidSubThresholdBidThrowsException() {
        when(stateRepository.findByAuctionId(1L)).thenReturn(Optional.of(sampleState));

        // Current highest is 100, min increment is 10 -> Minimum required is 110. Offered 105.
        PlaceBidRequest request = PlaceBidRequest.builder()
                .auctionId(1L)
                .bidAmount(new BigDecimal("105.00"))
                .build();

        assertThrows(SubThresholdBidException.class, () -> biddingService.placeBid(request, 30L));
    }

    @Test
    void testCalculateClearanceReserveMet() {
        when(stateRepository.findByAuctionId(1L)).thenReturn(Optional.of(sampleState));

        LocalDateTime t1 = LocalDateTime.now().minusMinutes(10);
        LocalDateTime t2 = LocalDateTime.now().minusMinutes(5);

        Bid topBid = Bid.builder()
                .id(1L)
                .auctionId(1L)
                .bidderId(50L)
                .bidAmount(new BigDecimal("250.00"))
                .status(BidStatus.WINNING)
                .bidTimestamp(t2)
                .build();

        Bid secondBid = Bid.builder()
                .id(2L)
                .auctionId(1L)
                .bidderId(40L)
                .bidAmount(new BigDecimal("210.00"))
                .status(BidStatus.OUTBID)
                .bidTimestamp(t1)
                .build();

        when(bidRepository.findByAuctionIdOrderByBidAmountDescBidTimestampAsc(1L))
                .thenReturn(Arrays.asList(topBid, secondBid));

        ClearanceResultDto result = biddingService.calculateClearance(1L);

        assertNotNull(result);
        assertEquals(50L, result.getWinningBidderId());
        assertEquals(new BigDecimal("250.00"), result.getWinningAmount());
        assertTrue(result.isReserveMet());
        assertEquals("CLEARED", result.getClearanceStatus());
    }

    @Test
    void testCalculateClearanceReserveNotMet() {
        when(stateRepository.findByAuctionId(1L)).thenReturn(Optional.of(sampleState));

        Bid topBid = Bid.builder()
                .id(1L)
                .auctionId(1L)
                .bidderId(50L)
                .bidAmount(new BigDecimal("150.00")) // Reserve is 200
                .status(BidStatus.WINNING)
                .bidTimestamp(LocalDateTime.now())
                .build();

        when(bidRepository.findByAuctionIdOrderByBidAmountDescBidTimestampAsc(1L))
                .thenReturn(List.of(topBid));

        ClearanceResultDto result = biddingService.calculateClearance(1L);

        assertNotNull(result);
        assertNull(result.getWinningBidderId());
        assertFalse(result.isReserveMet());
        assertEquals("RESERVE_NOT_MET", result.getClearanceStatus());
    }
}

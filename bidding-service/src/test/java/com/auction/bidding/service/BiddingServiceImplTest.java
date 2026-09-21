package com.auction.bidding.service;

import com.auction.bidding.client.AuctionClient;
import com.auction.bidding.dto.AuctionDto;
import com.auction.bidding.dto.BidResponse;
import com.auction.bidding.dto.ClearanceResultDto;
import com.auction.bidding.dto.PlaceBidRequest;
import com.auction.bidding.entity.AuctionBidState;
import com.auction.bidding.entity.Bid;
import com.auction.bidding.entity.BidStatus;
import com.auction.bidding.exception.AuctionNotActiveException;
import com.auction.bidding.exception.ResourceNotFoundException;
import com.auction.bidding.exception.SellerCannotBidException;
import com.auction.bidding.exception.SubThresholdBidException;
import com.auction.bidding.repository.AuctionBidStateRepository;
import com.auction.bidding.repository.BidRepository;
import com.auction.bidding.service.impl.BiddingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    private AuctionDto sampleAuctionDto;
    private AuctionBidState sampleState;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        sampleAuctionDto = AuctionDto.builder()
                .id(1L)
                .title("Rolex Submariner")
                .startingPrice(new BigDecimal("100.00"))
                .currentHighestBid(new BigDecimal("100.00"))
                .minBidIncrement(new BigDecimal("10.00"))
                .reservePrice(new BigDecimal("200.00"))
                .sellerId(10L)
                .status("ACTIVE")
                .startTime(now.minusHours(1))
                .endTime(now.plusDays(2))
                .build();

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
    @DisplayName("Place bid succeeds when amount exceeds current highest bid by minimum increment")
    void testPlaceBidSuccess() {
        when(auctionClient.getAuctionById(1L)).thenReturn(sampleAuctionDto);
        when(stateRepository.findByAuctionId(1L)).thenReturn(Optional.of(sampleState));

        Bid savedBid = Bid.builder()
                .id(101L)
                .auctionId(1L)
                .bidderId(30L)
                .amount(new BigDecimal("120.00"))
                .status(BidStatus.ACCEPTED)
                .acceptedAt(LocalDateTime.now())
                .build();

        when(bidRepository.save(any(Bid.class))).thenReturn(savedBid);
        when(stateRepository.save(any(AuctionBidState.class))).thenReturn(sampleState);

        PlaceBidRequest request = PlaceBidRequest.builder()
                .auctionId(1L)
                .bidAmount(new BigDecimal("120.00"))
                .build();

        BidResponse response = biddingService.placeBid(request, 30L);

        assertNotNull(response);
        assertEquals(new BigDecimal("120.00"), response.getAmount());
        assertEquals(30L, response.getBidderId());
        assertEquals(BidStatus.ACCEPTED, response.getStatus());
        verify(bidRepository, times(1)).save(any(Bid.class));
    }

    @Test
    @DisplayName("Place bid fails when auction does not exist")
    void testPlaceBidAuctionNotFound() {
        when(auctionClient.getAuctionById(999L)).thenReturn(null);

        PlaceBidRequest request = PlaceBidRequest.builder()
                .auctionId(999L)
                .bidAmount(new BigDecimal("120.00"))
                .build();

        assertThrows(ResourceNotFoundException.class, () -> biddingService.placeBid(request, 30L));
    }

    @Test
    @DisplayName("Place bid fails when auction is not ACTIVE (SCHEDULED, CLOSED, etc.)")
    void testPlaceBidAuctionNotActive() {
        sampleAuctionDto.setStatus("CLOSED");
        when(auctionClient.getAuctionById(1L)).thenReturn(sampleAuctionDto);

        PlaceBidRequest request = PlaceBidRequest.builder()
                .auctionId(1L)
                .bidAmount(new BigDecimal("120.00"))
                .build();

        assertThrows(AuctionNotActiveException.class, () -> biddingService.placeBid(request, 30L));
    }

    @Test
    @DisplayName("Place bid fails when current time is after endTime")
    void testPlaceBidAuctionExpired() {
        sampleAuctionDto.setEndTime(LocalDateTime.now().minusMinutes(5));
        when(auctionClient.getAuctionById(1L)).thenReturn(sampleAuctionDto);

        PlaceBidRequest request = PlaceBidRequest.builder()
                .auctionId(1L)
                .bidAmount(new BigDecimal("120.00"))
                .build();

        assertThrows(AuctionNotActiveException.class, () -> biddingService.placeBid(request, 30L));
    }

    @Test
    @DisplayName("Place bid fails when bidder is the seller")
    void testPlaceBidSellerCannotBid() {
        when(auctionClient.getAuctionById(1L)).thenReturn(sampleAuctionDto);

        PlaceBidRequest request = PlaceBidRequest.builder()
                .auctionId(1L)
                .bidAmount(new BigDecimal("150.00"))
                .build();

        assertThrows(SellerCannotBidException.class, () -> biddingService.placeBid(request, 10L)); // sellerId is 10
    }

    @Test
    @DisplayName("Place bid fails when amount is below minimum required increment threshold")
    void testPlaceBidSubThresholdBid() {
        when(auctionClient.getAuctionById(1L)).thenReturn(sampleAuctionDto);
        when(stateRepository.findByAuctionId(1L)).thenReturn(Optional.of(sampleState));

        // Current highest is 100, min increment is 10 -> Minimum required is 110. Offered 105.
        PlaceBidRequest request = PlaceBidRequest.builder()
                .auctionId(1L)
                .bidAmount(new BigDecimal("105.00"))
                .build();

        assertThrows(SubThresholdBidException.class, () -> biddingService.placeBid(request, 30L));
    }

    @Test
    @DisplayName("Get bid by ID returns matching BidResponse")
    void testGetBidByIdSuccess() {
        Bid bid = Bid.builder()
                .id(1L)
                .auctionId(10L)
                .bidderId(5L)
                .amount(new BigDecimal("150.00"))
                .status(BidStatus.ACCEPTED)
                .acceptedAt(LocalDateTime.now())
                .build();

        when(bidRepository.findById(1L)).thenReturn(Optional.of(bid));

        BidResponse response = biddingService.getBidById(1L);
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(new BigDecimal("150.00"), response.getAmount());
    }

    @Test
    @DisplayName("Get highest bid returns current highest accepted bid")
    void testGetHighestBidSuccess() {
        Bid highestBid = Bid.builder()
                .id(5L)
                .auctionId(1L)
                .bidderId(30L)
                .amount(new BigDecimal("220.00"))
                .status(BidStatus.ACCEPTED)
                .acceptedAt(LocalDateTime.now())
                .build();

        when(bidRepository.findHighestAcceptedBid(1L)).thenReturn(Optional.of(highestBid));

        BidResponse response = biddingService.getHighestBid(1L);
        assertNotNull(response);
        assertEquals(new BigDecimal("220.00"), response.getAmount());
        assertEquals(30L, response.getBidderId());
    }

    @Test
    @DisplayName("Get bids by auction returns bid history ordered by acceptedAt descending")
    void testGetBidsByAuction() {
        Bid bid1 = Bid.builder().id(1L).auctionId(1L).bidderId(10L).amount(new BigDecimal("100.00")).status(BidStatus.ACCEPTED).acceptedAt(LocalDateTime.now()).build();
        Bid bid2 = Bid.builder().id(2L).auctionId(1L).bidderId(20L).amount(new BigDecimal("110.00")).status(BidStatus.ACCEPTED).acceptedAt(LocalDateTime.now().plusMinutes(1)).build();

        when(bidRepository.findByAuctionIdOrderByAcceptedAtDesc(1L)).thenReturn(List.of(bid2, bid1));

        List<BidResponse> results = biddingService.getBidsByAuction(1L);
        assertEquals(2, results.size());
        assertEquals(new BigDecimal("110.00"), results.get(0).getAmount());
    }
}

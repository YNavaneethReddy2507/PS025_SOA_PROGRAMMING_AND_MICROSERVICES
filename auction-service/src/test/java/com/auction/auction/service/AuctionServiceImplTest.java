package com.auction.auction.service;

import com.auction.auction.client.BiddingClient;
import com.auction.auction.client.PaymentClient;
import com.auction.auction.dto.*;
import com.auction.auction.entity.Auction;
import com.auction.auction.entity.AuctionStatus;
import com.auction.auction.exception.InvalidAuctionStateException;
import com.auction.auction.exception.ResourceNotFoundException;
import com.auction.auction.exception.SellerCannotBidException;
import com.auction.auction.exception.UnauthorizedActionException;
import com.auction.auction.repository.AuctionRepository;
import com.auction.auction.service.impl.AuctionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
                .currentPrice(new BigDecimal("100.00"))
                .minimumIncrement(new BigDecimal("10.00"))
                .reservePrice(new BigDecimal("200.00"))
                .sellerId(10L)
                .status(AuctionStatus.ACTIVE)
                .startTime(now)
                .endTime(now.plusDays(3))
                .version(1L)
                .build();

        createRequest = CreateAuctionRequest.builder()
                .title("Vintage Watch")
                .description("Rare 1960s watch")
                .category("Collectibles")
                .startingPrice(new BigDecimal("100.00"))
                .reservePrice(new BigDecimal("200.00"))
                .minimumIncrement(new BigDecimal("10.00"))
                .startTime(now)
                .endTime(now.plusDays(3))
                .build();
    }

    @Test
    @DisplayName("Create auction succeeds with valid prices, schedule, and seller")
    void testCreateAuctionSuccess() {
        when(auctionRepository.save(any(Auction.class))).thenReturn(sampleAuction);

        AuctionResponse response = auctionService.createAuction(createRequest, 10L);

        assertNotNull(response);
        assertEquals("Vintage Watch", response.getTitle());
        assertEquals(new BigDecimal("100.00"), response.getStartingPrice());
        assertEquals(new BigDecimal("10.00"), response.getMinimumIncrement());
        assertEquals(10L, response.getSellerId());
        verify(auctionRepository, times(1)).save(any(Auction.class));
    }

    @Test
    @DisplayName("Create auction fails when startingPrice <= 0")
    void testCreateAuctionInvalidStartingPrice() {
        createRequest.setStartingPrice(BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class, () -> auctionService.createAuction(createRequest, 10L));

        createRequest.setStartingPrice(new BigDecimal("-10.00"));
        assertThrows(IllegalArgumentException.class, () -> auctionService.createAuction(createRequest, 10L));
    }

    @Test
    @DisplayName("Create auction fails when minimumIncrement <= 0")
    void testCreateAuctionInvalidMinimumIncrement() {
        createRequest.setMinimumIncrement(BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class, () -> auctionService.createAuction(createRequest, 10L));
    }

    @Test
    @DisplayName("Create auction fails when endTime is before or equal to startTime")
    void testCreateAuctionInvalidTimeRange() {
        createRequest.setEndTime(createRequest.getStartTime().minusHours(1));
        assertThrows(IllegalArgumentException.class, () -> auctionService.createAuction(createRequest, 10L));

        createRequest.setEndTime(createRequest.getStartTime());
        assertThrows(IllegalArgumentException.class, () -> auctionService.createAuction(createRequest, 10L));
    }

    @Test
    @DisplayName("Get auction by ID returns matching DTO")
    void testGetAuctionByIdSuccess() {
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(sampleAuction));

        AuctionResponse response = auctionService.getAuctionById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Vintage Watch", response.getTitle());
        assertEquals(new BigDecimal("100.00"), response.getCurrentPrice());
    }

    @Test
    @DisplayName("Get auction by ID throws ResourceNotFoundException for unknown ID")
    void testGetAuctionByIdNotFound() {
        when(auctionRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> auctionService.getAuctionById(999L));
    }

    @Test
    @DisplayName("Update auction succeeds for owner")
    void testUpdateAuctionSuccess() {
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(sampleAuction));
        when(auctionRepository.save(any(Auction.class))).thenReturn(sampleAuction);

        UpdateAuctionRequest updateReq = UpdateAuctionRequest.builder()
                .title("Updated Watch")
                .minimumIncrement(new BigDecimal("15.00"))
                .build();

        AuctionResponse response = auctionService.updateAuction(1L, updateReq, 10L, "ROLE_SELLER");
        assertNotNull(response);
        verify(auctionRepository, times(1)).save(any(Auction.class));
    }

    @Test
    @DisplayName("Update auction fails if auction is CLOSED (CLOSED auction cannot be modified)")
    void testUpdateAuctionFailsWhenClosed() {
        sampleAuction.setStatus(AuctionStatus.CLOSED);
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(sampleAuction));

        UpdateAuctionRequest updateReq = UpdateAuctionRequest.builder().title("New Title").build();
        InvalidAuctionStateException ex = assertThrows(InvalidAuctionStateException.class,
                () -> auctionService.updateAuction(1L, updateReq, 10L, "ROLE_SELLER"));
        assertTrue(ex.getMessage().contains("CLOSED auction cannot be modified"));
    }

    @Test
    @DisplayName("Start auction transitions SCHEDULED to ACTIVE")
    void testStartAuctionSuccess() {
        sampleAuction.setStatus(AuctionStatus.SCHEDULED);
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(sampleAuction));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        AuctionResponse response = auctionService.startAuction(1L, 10L, "ROLE_SELLER");
        assertNotNull(response);
        assertEquals(AuctionStatus.ACTIVE, response.getStatus());
    }

    @Test
    @DisplayName("Start auction fails if CANCELLED (CANCELLED auction cannot be restarted)")
    void testStartAuctionFailsWhenCancelled() {
        sampleAuction.setStatus(AuctionStatus.CANCELLED);
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(sampleAuction));

        InvalidAuctionStateException ex = assertThrows(InvalidAuctionStateException.class,
                () -> auctionService.startAuction(1L, 10L, "ROLE_SELLER"));
        assertTrue(ex.getMessage().contains("CANCELLED auction cannot be restarted"));
    }

    @Test
    @DisplayName("Close auction transitions ACTIVE to CLOSED")
    void testCloseAuctionSuccess() {
        sampleAuction.setStatus(AuctionStatus.ACTIVE);
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(sampleAuction));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        AuctionResponse response = auctionService.closeAuction(1L, 10L, "ROLE_SELLER");
        assertNotNull(response);
        assertEquals(AuctionStatus.CLOSED, response.getStatus());
    }

    @Test
    @DisplayName("Cancel auction succeeds by seller")
    void testCancelAuctionSuccessBySeller() {
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(sampleAuction));
        when(auctionRepository.save(any(Auction.class))).thenReturn(sampleAuction);

        AuctionResponse response = auctionService.cancelAuction(1L, 10L, "ROLE_SELLER");

        assertNotNull(response);
        verify(auctionRepository, times(1)).save(any(Auction.class));
    }

    @Test
    @DisplayName("Cancel auction forbidden for unauthorized user")
    void testCancelAuctionForbiddenForOtherUser() {
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(sampleAuction));
        assertThrows(UnauthorizedActionException.class, () -> auctionService.cancelAuction(1L, 99L, "ROLE_SELLER"));
    }

    @Test
    @DisplayName("Delete auction succeeds for non-active/non-closed auction")
    void testDeleteAuctionSuccess() {
        sampleAuction.setStatus(AuctionStatus.SCHEDULED);
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(sampleAuction));

        assertDoesNotThrow(() -> auctionService.deleteAuction(1L, 10L, "ROLE_SELLER"));
        verify(auctionRepository, times(1)).delete(sampleAuction);
    }

    @Test
    @DisplayName("Delete auction fails if auction is ACTIVE or CLOSED")
    void testDeleteAuctionFailsWhenActiveOrClosed() {
        sampleAuction.setStatus(AuctionStatus.ACTIVE);
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(sampleAuction));
        assertThrows(InvalidAuctionStateException.class, () -> auctionService.deleteAuction(1L, 10L, "ROLE_SELLER"));

        sampleAuction.setStatus(AuctionStatus.CLOSED);
        assertThrows(InvalidAuctionStateException.class, () -> auctionService.deleteAuction(1L, 10L, "ROLE_SELLER"));
    }

    @Test
    @DisplayName("Seller cannot bid on own auction validation fails")
    void testSellerCannotBidOnOwnAuction() {
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(sampleAuction));

        SellerCannotBidException ex = assertThrows(SellerCannotBidException.class,
                () -> auctionService.validateBidderNotSeller(1L, 10L));
        assertTrue(ex.getMessage().contains("Seller cannot bid on own auction"));

        UpdateHighestBidRequest bidRequest = UpdateHighestBidRequest.builder()
                .bidderId(10L) // seller
                .amount(new BigDecimal("150.00"))
                .build();
        assertThrows(SellerCannotBidException.class, () -> auctionService.updateHighestBid(1L, bidRequest));
    }
}

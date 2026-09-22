package com.auction.auction.security;

import com.auction.auction.dto.UpdateAuctionRequest;
import com.auction.auction.entity.Auction;
import com.auction.auction.entity.AuctionStatus;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuctionSecurityTest {

    @Mock
    private AuctionRepository auctionRepository;

    @InjectMocks
    private AuctionServiceImpl auctionService;

    private Auction sampleAuction;

    @BeforeEach
    void setUp() {
        sampleAuction = Auction.builder()
                .id(1L)
                .title("Rolex Submariner")
                .description("Luxury watch")
                .category("Watches")
                .startingPrice(new BigDecimal("5000.00"))
                .currentPrice(new BigDecimal("5000.00"))
                .minimumIncrement(new BigDecimal("100.00"))
                .sellerId(10L)
                .status(AuctionStatus.SCHEDULED)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusDays(5))
                .build();
    }

    @Test
    @DisplayName("Non-owner user cannot update another user's auction")
    void testNonOwnerCannotUpdateAuction() {
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(sampleAuction));

        UpdateAuctionRequest updateReq = UpdateAuctionRequest.builder()
                .title("Malicious Title")
                .build();

        assertThrows(UnauthorizedActionException.class,
                () -> auctionService.updateAuction(1L, updateReq, 999L, "ROLE_USER"));
    }

    @Test
    @DisplayName("Unauthenticated caller (null userId) cannot update auction")
    void testUnauthenticatedUserCannotUpdateAuction() {
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(sampleAuction));

        UpdateAuctionRequest updateReq = UpdateAuctionRequest.builder()
                .title("Hacked Title")
                .build();

        assertThrows(UnauthorizedActionException.class,
                () -> auctionService.updateAuction(1L, updateReq, null, "ROLE_USER"));
    }

    @Test
    @DisplayName("Admin can update any user's auction")
    void testAdminCanUpdateAnyAuction() {
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(sampleAuction));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateAuctionRequest updateReq = UpdateAuctionRequest.builder()
                .title("Admin Approved Title")
                .build();

        assertDoesNotThrow(() -> auctionService.updateAuction(1L, updateReq, 999L, "ROLE_ADMIN"));
    }

    @Test
    @DisplayName("Non-owner user cannot delete another user's auction")
    void testNonOwnerCannotDeleteAuction() {
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(sampleAuction));

        assertThrows(UnauthorizedActionException.class,
                () -> auctionService.deleteAuction(1L, 999L, "ROLE_USER"));
    }

    @Test
    @DisplayName("Non-owner user cannot start another user's auction")
    void testNonOwnerCannotStartAuction() {
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(sampleAuction));

        assertThrows(UnauthorizedActionException.class,
                () -> auctionService.startAuction(1L, 999L, "ROLE_USER"));
    }

    @Test
    @DisplayName("Non-owner user cannot cancel another user's auction")
    void testNonOwnerCannotCancelAuction() {
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(sampleAuction));

        assertThrows(UnauthorizedActionException.class,
                () -> auctionService.cancelAuction(1L, 999L, "ROLE_USER"));
    }
}

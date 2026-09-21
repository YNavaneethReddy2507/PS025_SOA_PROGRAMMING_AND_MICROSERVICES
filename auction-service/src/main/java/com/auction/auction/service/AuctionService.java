package com.auction.auction.service;

import com.auction.auction.dto.*;
import com.auction.auction.entity.AuctionStatus;

import java.util.List;

public interface AuctionService {
    AuctionResponse createAuction(CreateAuctionRequest request, Long sellerId);
    AuctionResponse getAuctionById(Long id);
    List<AuctionResponse> getAuctions(AuctionStatus status, String category);
    List<AuctionResponse> getAuctionsBySeller(Long sellerId);
    AuctionResponse updateAuction(Long id, UpdateAuctionRequest request, Long userId, String userRole);
    void deleteAuction(Long id, Long userId, String userRole);

    // Lifecycle operations
    AuctionResponse startAuction(Long id, Long userId, String userRole);
    AuctionResponse closeAuction(Long id, Long userId, String userRole);
    AuctionResponse cancelAuction(Long id, Long userId, String userRole);
    AuctionResponse activateAuction(Long id, Long userId, String userRole);

    // High concurrency bid updates and clearance
    AuctionResponse updateHighestBid(Long id, UpdateHighestBidRequest request);
    ClearanceResultDto closeAndClearAuction(Long id);
    void checkAndCloseExpiredAuctions();
    void validateBidderNotSeller(Long auctionId, Long bidderId);
}

package com.auction.bidding.service;

import com.auction.bidding.dto.BidResponse;
import com.auction.bidding.dto.ClearanceResultDto;
import com.auction.bidding.dto.PlaceBidRequest;

import java.util.List;

public interface BiddingService {
    BidResponse placeBid(PlaceBidRequest request, Long bidderId);
    List<BidResponse> getBidsByAuction(Long auctionId);
    ClearanceResultDto calculateClearance(Long auctionId);
    BidResponse getHighestBid(Long auctionId);
}

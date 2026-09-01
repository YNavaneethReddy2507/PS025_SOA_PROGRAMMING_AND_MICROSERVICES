package com.auction.bidding.controller;

import com.auction.bidding.dto.BidResponse;
import com.auction.bidding.dto.ClearanceResultDto;
import com.auction.bidding.dto.PlaceBidRequest;
import com.auction.bidding.service.BiddingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bids")
public class BiddingController {

    private static final Logger log = LoggerFactory.getLogger(BiddingController.class);

    private final BiddingService biddingService;

    public BiddingController(BiddingService biddingService) {
        this.biddingService = biddingService;
    }

    @PostMapping
    public ResponseEntity<BidResponse> placeBid(
            @Valid @RequestBody PlaceBidRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        Long bidderId = headerUserId != null ? headerUserId : 2L;
        log.info("Received bid placement from bidder ID: {}", bidderId);
        BidResponse response = biddingService.placeBid(request, bidderId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/auction/{auctionId}")
    public ResponseEntity<List<BidResponse>> getBidsByAuction(@PathVariable("auctionId") Long auctionId) {
        List<BidResponse> bids = biddingService.getBidsByAuction(auctionId);
        return ResponseEntity.ok(bids);
    }

    @GetMapping("/auction/{auctionId}/highest")
    public ResponseEntity<BidResponse> getHighestBid(@PathVariable("auctionId") Long auctionId) {
        BidResponse response = biddingService.getHighestBid(auctionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/auction/{auctionId}/clearance")
    public ResponseEntity<ClearanceResultDto> calculateClearance(@PathVariable("auctionId") Long auctionId) {
        ClearanceResultDto result = biddingService.calculateClearance(auctionId);
        return ResponseEntity.ok(result);
    }
}

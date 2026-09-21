package com.auction.bidding.controller;

import com.auction.bidding.dto.BidResponse;
import com.auction.bidding.dto.ClearanceResultDto;
import com.auction.bidding.dto.PlaceBidRequest;
import com.auction.bidding.exception.UnauthorizedException;
import com.auction.bidding.security.JwtUtil;
import com.auction.bidding.service.BiddingService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BiddingController {

    private static final Logger log = LoggerFactory.getLogger(BiddingController.class);

    private final BiddingService biddingService;
    private final JwtUtil jwtUtil;

    public BiddingController(BiddingService biddingService, JwtUtil jwtUtil) {
        this.biddingService = biddingService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping({"/api/bids", "/api/v1/bids"})
    public ResponseEntity<BidResponse> placeBid(
            @Valid @RequestBody PlaceBidRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        Long bidderId = extractBidderId(headerUserId, authHeader);
        log.info("Received authenticated bid placement from bidder ID: {}", bidderId);
        BidResponse response = biddingService.placeBid(request, bidderId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping({"/api/bids/{id}", "/api/v1/bids/{id}"})
    public ResponseEntity<BidResponse> getBidById(@PathVariable("id") Long id) {
        BidResponse response = biddingService.getBidById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/api/auctions/{auctionId}/bids", "/api/bids/auction/{auctionId}", "/api/v1/bids/auction/{auctionId}"})
    public ResponseEntity<List<BidResponse>> getBidsByAuction(@PathVariable("auctionId") Long auctionId) {
        List<BidResponse> bids = biddingService.getBidsByAuction(auctionId);
        return ResponseEntity.ok(bids);
    }

    @GetMapping({"/api/auctions/{auctionId}/highest", "/api/bids/auction/{auctionId}/highest", "/api/v1/bids/auction/{auctionId}/highest"})
    public ResponseEntity<BidResponse> getHighestBid(@PathVariable("auctionId") Long auctionId) {
        BidResponse response = biddingService.getHighestBid(auctionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/api/bids/auction/{auctionId}/clearance", "/api/v1/bids/auction/{auctionId}/clearance"})
    public ResponseEntity<ClearanceResultDto> calculateClearance(@PathVariable("auctionId") Long auctionId) {
        ClearanceResultDto result = biddingService.calculateClearance(auctionId);
        return ResponseEntity.ok(result);
    }

    private Long extractBidderId(Long headerUserId, String authHeader) {
        if (headerUserId != null) {
            return headerUserId;
        }
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                Claims claims = jwtUtil.extractAllClaims(token);
                Object userIdClaim = claims.get("userId");
                if (userIdClaim != null) {
                    return Long.valueOf(userIdClaim.toString());
                }
            }
        }
        throw new UnauthorizedException("Authentication token or bidder identity is missing or invalid");
    }
}

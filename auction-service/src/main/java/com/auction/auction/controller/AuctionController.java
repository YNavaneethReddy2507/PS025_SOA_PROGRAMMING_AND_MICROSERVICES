package com.auction.auction.controller;

import com.auction.auction.dto.*;
import com.auction.auction.entity.AuctionStatus;
import com.auction.auction.service.AuctionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auctions")
public class AuctionController {

    private static final Logger log = LoggerFactory.getLogger(AuctionController.class);

    private final AuctionService auctionService;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @PostMapping
    public ResponseEntity<AuctionResponse> createAuction(
            @Valid @RequestBody CreateAuctionRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        Long sellerId = headerUserId != null ? headerUserId : 1L;
        log.info("Creating auction request from seller ID: {}", sellerId);
        AuctionResponse response = auctionService.createAuction(request, sellerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuctionResponse> getAuctionById(@PathVariable("id") Long id) {
        AuctionResponse response = auctionService.getAuctionById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<AuctionResponse>> getAuctions(
            @RequestParam(value = "status", required = false) AuctionStatus status,
            @RequestParam(value = "category", required = false) String category) {
        List<AuctionResponse> responses = auctionService.getAuctions(status, category);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<AuctionResponse>> getAuctionsBySeller(@PathVariable("sellerId") Long sellerId) {
        List<AuctionResponse> responses = auctionService.getAuctionsBySeller(sellerId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AuctionResponse> updateAuction(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateAuctionRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        AuctionResponse response = auctionService.updateAuction(id, request, userId, userRole);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<AuctionResponse> activateAuction(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        AuctionResponse response = auctionService.activateAuction(id, userId, userRole);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<AuctionResponse> cancelAuction(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        AuctionResponse response = auctionService.cancelAuction(id, userId, userRole);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/highest-bid")
    public ResponseEntity<AuctionResponse> updateHighestBid(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateHighestBidRequest request) {
        AuctionResponse response = auctionService.updateHighestBid(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/clear")
    public ResponseEntity<ClearanceResultDto> closeAndClearAuction(@PathVariable("id") Long id) {
        ClearanceResultDto response = auctionService.closeAndClearAuction(id);
        return ResponseEntity.ok(response);
    }
}

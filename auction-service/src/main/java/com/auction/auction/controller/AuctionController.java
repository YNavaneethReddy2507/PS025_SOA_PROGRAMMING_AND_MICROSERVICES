package com.auction.auction.controller;

import com.auction.auction.dto.*;
import com.auction.auction.entity.AuctionStatus;
import com.auction.auction.exception.UnauthorizedActionException;
import com.auction.auction.security.JwtUtil;
import com.auction.auction.service.AuctionService;
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
@RequestMapping({"/api/auctions", "/api/v1/auctions"})
public class AuctionController {

    private static final Logger log = LoggerFactory.getLogger(AuctionController.class);

    private final AuctionService auctionService;
    private final JwtUtil jwtUtil;

    public AuctionController(AuctionService auctionService, JwtUtil jwtUtil) {
        this.auctionService = auctionService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
    public ResponseEntity<AuctionResponse> createAuction(
            @Valid @RequestBody CreateAuctionRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        Long sellerId = extractUserId(headerUserId, authHeader);
        log.info("Creating auction request from seller ID: {}", sellerId);
        AuctionResponse response = auctionService.createAuction(request, sellerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AuctionResponse>> getAuctions(
            @RequestParam(value = "status", required = false) AuctionStatus status,
            @RequestParam(value = "category", required = false) String category) {
        List<AuctionResponse> responses = auctionService.getAuctions(status, category);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuctionResponse> getAuctionById(@PathVariable("id") Long id) {
        AuctionResponse response = auctionService.getAuctionById(id);
        return ResponseEntity.ok(response);
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
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        Long userId = extractUserId(headerUserId, authHeader);
        String userRole = extractUserRole(headerRole, authHeader);
        AuctionResponse response = auctionService.updateAuction(id, request, userId, userRole);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAuction(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        Long userId = extractUserId(headerUserId, authHeader);
        String userRole = extractUserRole(headerRole, authHeader);
        auctionService.deleteAuction(id, userId, userRole);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<AuctionResponse> startAuction(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        Long userId = extractUserId(headerUserId, authHeader);
        String userRole = extractUserRole(headerRole, authHeader);
        AuctionResponse response = auctionService.startAuction(id, userId, userRole);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<AuctionResponse> activateAuction(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        Long userId = extractUserId(headerUserId, authHeader);
        String userRole = extractUserRole(headerRole, authHeader);
        AuctionResponse response = auctionService.activateAuction(id, userId, userRole);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<AuctionResponse> closeAuction(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        Long userId = extractUserId(headerUserId, authHeader);
        String userRole = extractUserRole(headerRole, authHeader);
        AuctionResponse response = auctionService.closeAuction(id, userId, userRole);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<AuctionResponse> cancelAuction(
            @PathVariable("id") Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestHeader(value = "X-User-Role", required = false) String headerRole,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        Long userId = extractUserId(headerUserId, authHeader);
        String userRole = extractUserRole(headerRole, authHeader);
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

    private Long extractUserId(Long headerUserId, String authHeader) {
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
        // Fallback default for testing or raise unauthorized
        throw new UnauthorizedActionException("Authentication token or user identity is missing or invalid");
    }

    private String extractUserRole(String headerRole, String authHeader) {
        if (headerRole != null) {
            return headerRole;
        }
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                Claims claims = jwtUtil.extractAllClaims(token);
                Object roleClaim = claims.get("role");
                if (roleClaim != null) {
                    return roleClaim.toString();
                }
            }
        }
        return "ROLE_USER";
    }
}

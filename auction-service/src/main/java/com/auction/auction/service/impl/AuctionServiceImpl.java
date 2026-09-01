package com.auction.auction.service.impl;

import com.auction.auction.client.BiddingClient;
import com.auction.auction.client.PaymentClient;
import com.auction.auction.dto.*;
import com.auction.auction.entity.Auction;
import com.auction.auction.entity.AuctionStatus;
import com.auction.auction.exception.InvalidAuctionStateException;
import com.auction.auction.exception.ResourceNotFoundException;
import com.auction.auction.exception.UnauthorizedActionException;
import com.auction.auction.repository.AuctionRepository;
import com.auction.auction.service.AuctionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuctionServiceImpl implements AuctionService {

    private static final Logger log = LoggerFactory.getLogger(AuctionServiceImpl.class);

    private final AuctionRepository auctionRepository;
    private final BiddingClient biddingClient;
    private final PaymentClient paymentClient;

    public AuctionServiceImpl(AuctionRepository auctionRepository, BiddingClient biddingClient, PaymentClient paymentClient) {
        this.auctionRepository = auctionRepository;
        this.biddingClient = biddingClient;
        this.paymentClient = paymentClient;
    }

    @Override
    @Transactional
    public AuctionResponse createAuction(CreateAuctionRequest request, Long sellerId) {
        log.info("Creating new auction with title: '{}' for seller ID: {}", request.getTitle(), sellerId);

        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new IllegalArgumentException("Auction end time must be after start time");
        }

        Auction auction = Auction.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .category(request.getCategory().trim())
                .startingPrice(request.getStartingPrice())
                .reservePrice(request.getReservePrice())
                .minBidIncrement(request.getMinBidIncrement())
                .currentHighestBid(request.getStartingPrice())
                .sellerId(sellerId)
                .status(AuctionStatus.ACTIVE) // active upon creation if start time is current
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();

        Auction savedAuction = auctionRepository.save(auction);
        log.info("Auction successfully created with ID: {}", savedAuction.getId());

        return mapToResponse(savedAuction);
    }

    @Override
    @Transactional(readOnly = true)
    public AuctionResponse getAuctionById(Long id) {
        Auction auction = findAuctionOrThrow(id);
        return mapToResponse(auction);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuctionResponse> getAuctions(AuctionStatus status, String category) {
        List<Auction> auctions;
        if (status != null && category != null) {
            auctions = auctionRepository.findByStatusAndCategory(status, category);
        } else if (status != null) {
            auctions = auctionRepository.findByStatus(status);
        } else if (category != null) {
            auctions = auctionRepository.findByCategory(category);
        } else {
            auctions = auctionRepository.findAll();
        }

        return auctions.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuctionResponse> getAuctionsBySeller(Long sellerId) {
        return auctionRepository.findBySellerId(sellerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AuctionResponse updateAuction(Long id, UpdateAuctionRequest request, Long userId, String userRole) {
        Auction auction = findAuctionOrThrow(id);
        verifyOwnershipOrAdmin(auction, userId, userRole);

        if (auction.getStatus() == AuctionStatus.SETTLED || auction.getStatus() == AuctionStatus.CANCELLED) {
            throw new InvalidAuctionStateException("Cannot update an auction that is already " + auction.getStatus());
        }

        if (request.getTitle() != null) auction.setTitle(request.getTitle().trim());
        if (request.getDescription() != null) auction.setDescription(request.getDescription());
        if (request.getCategory() != null) auction.setCategory(request.getCategory().trim());
        if (request.getReservePrice() != null) auction.setReservePrice(request.getReservePrice());
        if (request.getMinBidIncrement() != null) auction.setMinBidIncrement(request.getMinBidIncrement());
        if (request.getEndTime() != null) {
            if (request.getEndTime().isBefore(auction.getStartTime())) {
                throw new IllegalArgumentException("End time cannot be earlier than start time");
            }
            auction.setEndTime(request.getEndTime());
        }

        Auction updated = auctionRepository.save(auction);
        log.info("Updated auction ID: {}", updated.getId());
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public AuctionResponse activateAuction(Long id, Long userId, String userRole) {
        Auction auction = findAuctionOrThrow(id);
        verifyOwnershipOrAdmin(auction, userId, userRole);

        if (auction.getStatus() != AuctionStatus.DRAFT) {
            throw new InvalidAuctionStateException("Only DRAFT auctions can be activated. Current status: " + auction.getStatus());
        }

        auction.setStatus(AuctionStatus.ACTIVE);
        Auction saved = auctionRepository.save(auction);
        log.info("Activated auction ID: {}", saved.getId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public AuctionResponse cancelAuction(Long id, Long userId, String userRole) {
        Auction auction = findAuctionOrThrow(id);
        verifyOwnershipOrAdmin(auction, userId, userRole);

        if (auction.getStatus() == AuctionStatus.SETTLED || auction.getStatus() == AuctionStatus.CANCELLED) {
            throw new InvalidAuctionStateException("Auction is already finalized as " + auction.getStatus());
        }

        auction.setStatus(AuctionStatus.CANCELLED);
        Auction saved = auctionRepository.save(auction);
        log.info("Cancelled auction ID: {}", saved.getId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public AuctionResponse updateHighestBid(Long id, UpdateHighestBidRequest request) {
        Auction auction = findAuctionOrThrow(id);

        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new InvalidAuctionStateException("Cannot place bid on non-active auction. Current status: " + auction.getStatus());
        }

        auction.setCurrentHighestBid(request.getAmount());
        auction.setWinningBidderId(request.getBidderId());

        Auction saved = auctionRepository.save(auction);
        log.info("Updated highest bid for auction ID {} to amount {} by user {}", id, request.getAmount(), request.getBidderId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ClearanceResultDto closeAndClearAuction(Long id) {
        Auction auction = findAuctionOrThrow(id);

        if (auction.getStatus() == AuctionStatus.SETTLED || auction.getStatus() == AuctionStatus.CANCELLED) {
            throw new InvalidAuctionStateException("Auction has already been closed/settled or cancelled");
        }

        log.info("Initiating clearance for auction ID: {}", id);
        auction.setStatus(AuctionStatus.ENDED);

        // Inter-service call to bidding-service to calculate deterministic clearance
        ClearanceResultDto clearanceResult;
        try {
            clearanceResult = biddingClient.calculateClearance(id);
        } catch (Exception e) {
            log.warn("Could not fetch clearance from Bidding Service for auction {}: {}", id, e.getMessage());
            clearanceResult = ClearanceResultDto.builder()
                    .auctionId(id)
                    .winningBidderId(auction.getWinningBidderId())
                    .winningAmount(auction.getCurrentHighestBid())
                    .reserveMet(auction.getCurrentHighestBid() != null && auction.getCurrentHighestBid().compareTo(auction.getReservePrice()) >= 0)
                    .clearedAt(LocalDateTime.now())
                    .clearanceStatus("CLEARED_LOCAL")
                    .message("Local clearance fallback")
                    .build();
        }

        if (clearanceResult != null && clearanceResult.getWinningBidderId() != null && clearanceResult.isReserveMet()) {
            auction.setWinningBidderId(clearanceResult.getWinningBidderId());
            auction.setCurrentHighestBid(clearanceResult.getWinningAmount());

            // Trigger payment settlement
            try {
                PaymentSettlementRequest settlementRequest = PaymentSettlementRequest.builder()
                        .auctionId(id)
                        .sellerId(auction.getSellerId())
                        .buyerId(clearanceResult.getWinningBidderId())
                        .amount(clearanceResult.getWinningAmount())
                        .build();

                PaymentSettlementResponse paymentResponse = paymentClient.processSettlement(settlementRequest);
                auction.setStatus(AuctionStatus.SETTLED);
                log.info("Auction ID {} settled successfully with payment tx: {}", id, paymentResponse.getTransactionId());
            } catch (Exception e) {
                log.error("Payment settlement failed for auction ID {}: {}", id, e.getMessage());
                auction.setStatus(AuctionStatus.ENDED);
            }
        } else {
            auction.setStatus(AuctionStatus.ENDED);
            log.info("Auction ID {} closed without winning bid (reserve not met or no bids)", id);
        }

        auctionRepository.save(auction);
        return clearanceResult;
    }

    @Override
    @Transactional
    public void checkAndCloseExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<Auction> expired = auctionRepository.findExpiredAuctions(AuctionStatus.ACTIVE, now);

        if (!expired.isEmpty()) {
            log.info("Found {} expired auctions to close and clear", expired.size());
            for (Auction auction : expired) {
                try {
                    closeAndClearAuction(auction.getId());
                } catch (Exception e) {
                    log.error("Error clearing expired auction {}: {}", auction.getId(), e.getMessage());
                }
            }
        }
    }

    private Auction findAuctionOrThrow(Long id) {
        return auctionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found with ID: " + id));
    }

    private void verifyOwnershipOrAdmin(Auction auction, Long userId, String userRole) {
        if ("ROLE_ADMIN".equals(userRole) || "ADMIN".equals(userRole)) {
            return;
        }
        if (userId != null && !auction.getSellerId().equals(userId)) {
            throw new UnauthorizedActionException("You are not authorized to modify this auction");
        }
    }

    private AuctionResponse mapToResponse(Auction a) {
        return AuctionResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .description(a.getDescription())
                .category(a.getCategory())
                .startingPrice(a.getStartingPrice())
                .reservePrice(a.getReservePrice())
                .minBidIncrement(a.getMinBidIncrement())
                .currentHighestBid(a.getCurrentHighestBid())
                .winningBidderId(a.getWinningBidderId())
                .sellerId(a.getSellerId())
                .status(a.getStatus())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}

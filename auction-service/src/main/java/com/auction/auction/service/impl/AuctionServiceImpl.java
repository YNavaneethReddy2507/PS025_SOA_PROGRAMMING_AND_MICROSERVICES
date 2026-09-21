package com.auction.auction.service.impl;

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
import com.auction.auction.service.AuctionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class AuctionServiceImpl implements AuctionService {

    private static final Logger log = LoggerFactory.getLogger(AuctionServiceImpl.class);

    private final AuctionRepository auctionRepository;
    private final BiddingClient biddingClient;
    private final PaymentClient paymentClient;
    private final TransactionTemplate transactionTemplate;

    // Mutex lock per auction to serialize simultaneous state transitions and closures
    private final ConcurrentHashMap<Long, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

    @Autowired
    public AuctionServiceImpl(
            AuctionRepository auctionRepository,
            BiddingClient biddingClient,
            PaymentClient paymentClient,
            @Autowired(required = false) PlatformTransactionManager transactionManager
    ) {
        this.auctionRepository = auctionRepository;
        this.biddingClient = biddingClient;
        this.paymentClient = paymentClient;
        this.transactionTemplate = transactionManager != null ? new TransactionTemplate(transactionManager) : null;
    }

    public AuctionServiceImpl(AuctionRepository auctionRepository, BiddingClient biddingClient, PaymentClient paymentClient) {
        this(auctionRepository, biddingClient, paymentClient, null);
    }

    private <T> T executeInTransaction(Supplier<T> action) {
        if (transactionTemplate != null) {
            return transactionTemplate.execute(status -> action.get());
        } else {
            return action.get();
        }
    }

    @Override
    @Transactional
    public AuctionResponse createAuction(CreateAuctionRequest request, Long sellerId) {
        log.info("Creating new auction with title: '{}' for seller ID: {}", request.getTitle(), sellerId);

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be blank");
        }
        if (request.getStartingPrice() == null || request.getStartingPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Starting price must be greater than 0");
        }
        BigDecimal minInc = request.getMinimumIncrement();
        if (minInc == null || minInc.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Minimum increment must be greater than 0");
        }
        LocalDateTime start = request.getStartTime() != null ? request.getStartTime() : LocalDateTime.now();
        if (request.getEndTime() == null) {
            throw new IllegalArgumentException("End time is required");
        }
        if (request.getEndTime().isBefore(start) || request.getEndTime().isEqual(start)) {
            throw new IllegalArgumentException("Auction end time must be after start time");
        }

        AuctionStatus initialStatus = start.isAfter(LocalDateTime.now()) ? AuctionStatus.SCHEDULED : AuctionStatus.ACTIVE;

        Auction auction = Auction.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .category(request.getCategory())
                .startingPrice(request.getStartingPrice())
                .currentPrice(request.getStartingPrice())
                .minimumIncrement(minInc)
                .reservePrice(request.getReservePrice())
                .sellerId(sellerId)
                .status(initialStatus)
                .startTime(start)
                .endTime(request.getEndTime())
                .build();

        Auction savedAuction = auctionRepository.save(auction);
        log.info("Auction successfully created with ID: {} and status: {}", savedAuction.getId(), savedAuction.getStatus());

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
        ReentrantLock lock = auctionLocks.computeIfAbsent(id, k -> new ReentrantLock(true));
        lock.lock();
        try {
            Auction auction = findAuctionOrThrow(id);
            verifyOwnershipOrAdmin(auction, userId, userRole);

            if (auction.getStatus() == AuctionStatus.CLOSED) {
                throw new InvalidAuctionStateException("CLOSED auction cannot be modified");
            }
            if (auction.getStatus() == AuctionStatus.CANCELLED) {
                throw new InvalidAuctionStateException("CANCELLED auction cannot be modified");
            }

            if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
                auction.setTitle(request.getTitle().trim());
            }
            if (request.getDescription() != null) {
                auction.setDescription(request.getDescription());
            }
            if (request.getCategory() != null) {
                auction.setCategory(request.getCategory().trim());
            }
            if (request.getStartingPrice() != null) {
                if (request.getStartingPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Starting price must be greater than 0");
                }
                auction.setStartingPrice(request.getStartingPrice());
                if (auction.getWinnerId() == null) {
                    auction.setCurrentPrice(request.getStartingPrice());
                }
            }
            if (request.getMinimumIncrement() != null) {
                if (request.getMinimumIncrement().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Minimum increment must be greater than 0");
                }
                auction.setMinimumIncrement(request.getMinimumIncrement());
            }
            if (request.getReservePrice() != null) {
                if (request.getReservePrice().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Reserve price must be greater than 0");
                }
                auction.setReservePrice(request.getReservePrice());
            }
            if (request.getEndTime() != null) {
                if (request.getEndTime().isBefore(auction.getStartTime()) || request.getEndTime().isEqual(auction.getStartTime())) {
                    throw new IllegalArgumentException("End time must be after start time");
                }
                auction.setEndTime(request.getEndTime());
            }

            Auction updated = auctionRepository.save(auction);
            log.info("Updated auction ID: {}", updated.getId());
            return mapToResponse(updated);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional
    public void deleteAuction(Long id, Long userId, String userRole) {
        ReentrantLock lock = auctionLocks.computeIfAbsent(id, k -> new ReentrantLock(true));
        lock.lock();
        try {
            Auction auction = findAuctionOrThrow(id);
            verifyOwnershipOrAdmin(auction, userId, userRole);

            if (auction.getStatus() == AuctionStatus.CLOSED) {
                throw new InvalidAuctionStateException("CLOSED auction cannot be deleted");
            }
            if (auction.getStatus() == AuctionStatus.ACTIVE) {
                throw new InvalidAuctionStateException("ACTIVE auction cannot be deleted");
            }

            auctionRepository.delete(auction);
            log.info("Auction ID: {} successfully deleted by user: {}", id, userId);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional
    public AuctionResponse startAuction(Long id, Long userId, String userRole) {
        ReentrantLock lock = auctionLocks.computeIfAbsent(id, k -> new ReentrantLock(true));
        lock.lock();
        try {
            Auction auction = findAuctionOrThrow(id);
            verifyOwnershipOrAdmin(auction, userId, userRole);

            if (auction.getStatus() == AuctionStatus.CANCELLED) {
                throw new InvalidAuctionStateException("CANCELLED auction cannot be restarted");
            }
            if (auction.getStatus() == AuctionStatus.CLOSED) {
                throw new InvalidAuctionStateException("CLOSED auction cannot be modified");
            }
            if (auction.getStatus() == AuctionStatus.ACTIVE) {
                return mapToResponse(auction);
            }

            auction.setStatus(AuctionStatus.ACTIVE);
            Auction saved = auctionRepository.save(auction);
            log.info("Started auction ID: {} (status changed to ACTIVE)", saved.getId());
            return mapToResponse(saved);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional
    public AuctionResponse activateAuction(Long id, Long userId, String userRole) {
        return startAuction(id, userId, userRole);
    }

    @Override
    public AuctionResponse closeAuction(Long id, Long userId, String userRole) {
        ReentrantLock lock = auctionLocks.computeIfAbsent(id, k -> new ReentrantLock(true));
        lock.lock();
        try {
            return executeInTransaction(() -> {
                Auction auction = findAuctionOrThrow(id);
                verifyOwnershipOrAdmin(auction, userId, userRole);

                if (auction.getStatus() == AuctionStatus.CANCELLED) {
                    throw new InvalidAuctionStateException("Cannot close a CANCELLED auction");
                }
                // Idempotent closing: return existing closed auction
                if (auction.getStatus() == AuctionStatus.CLOSED) {
                    log.info("Auction ID {} is already CLOSED. Idempotent return.", id);
                    return mapToResponse(auction);
                }

                auction.setStatus(AuctionStatus.CLOSED);

                // Fetch deterministic winning bid from Bidding Service
                try {
                    ClearanceResultDto clearance = biddingClient.calculateClearance(id);
                    if (clearance != null && clearance.getWinningBidderId() != null) {
                        auction.setWinnerId(clearance.getWinningBidderId());
                        auction.setCurrentPrice(clearance.getWinningAmount());
                        log.info("Auction ID {} closed with winner: {}, amount: {}", id, clearance.getWinningBidderId(), clearance.getWinningAmount());
                    }
                } catch (Exception e) {
                    log.warn("Could not fetch clearance from Bidding Service during close for auction {}: {}", id, e.getMessage());
                }

                Auction saved = auctionRepository.save(auction);
                log.info("Closed auction ID: {}", saved.getId());
                return mapToResponse(saved);
            });
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional
    public AuctionResponse cancelAuction(Long id, Long userId, String userRole) {
        ReentrantLock lock = auctionLocks.computeIfAbsent(id, k -> new ReentrantLock(true));
        lock.lock();
        try {
            Auction auction = findAuctionOrThrow(id);
            verifyOwnershipOrAdmin(auction, userId, userRole);

            if (auction.getStatus() == AuctionStatus.CLOSED) {
                throw new InvalidAuctionStateException("CLOSED auction cannot be cancelled");
            }
            if (auction.getStatus() == AuctionStatus.CANCELLED) {
                throw new InvalidAuctionStateException("Auction is already CANCELLED");
            }

            auction.setStatus(AuctionStatus.CANCELLED);
            Auction saved = auctionRepository.save(auction);
            log.info("Cancelled auction ID: {}", saved.getId());
            return mapToResponse(saved);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public AuctionResponse updateHighestBid(Long id, UpdateHighestBidRequest request) {
        ReentrantLock lock = auctionLocks.computeIfAbsent(id, k -> new ReentrantLock(true));
        lock.lock();
        try {
            return executeInTransaction(() -> {
                Auction auction = findAuctionOrThrow(id);

                if (auction.getStatus() != AuctionStatus.ACTIVE) {
                    throw new InvalidAuctionStateException("Cannot place bid on non-active auction. Current status: " + auction.getStatus());
                }

                if (auction.getSellerId().equals(request.getBidderId())) {
                    throw new SellerCannotBidException("Seller cannot bid on own auction");
                }

                auction.setCurrentPrice(request.getAmount());
                auction.setWinnerId(request.getBidderId());

                Auction saved = auctionRepository.save(auction);
                log.info("Updated highest bid for auction ID {} to amount {} by user {}", id, request.getAmount(), request.getBidderId());
                return mapToResponse(saved);
            });
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateBidderNotSeller(Long auctionId, Long bidderId) {
        Auction auction = findAuctionOrThrow(auctionId);
        if (auction.getSellerId().equals(bidderId)) {
            throw new SellerCannotBidException("Seller cannot bid on own auction");
        }
    }

    @Override
    public ClearanceResultDto closeAndClearAuction(Long id) {
        ReentrantLock lock = auctionLocks.computeIfAbsent(id, k -> new ReentrantLock(true));
        lock.lock();
        try {
            return executeInTransaction(() -> {
                Auction auction = findAuctionOrThrow(id);

                // Idempotent: If already closed, return existing state
                if (auction.getStatus() == AuctionStatus.CLOSED) {
                    log.info("Auction {} is already CLOSED. Returning existing clearance.", id);
                    return ClearanceResultDto.builder()
                            .auctionId(id)
                            .winningBidderId(auction.getWinnerId())
                            .winningAmount(auction.getCurrentPrice())
                            .reserveMet(auction.getWinnerId() != null)
                            .clearedAt(LocalDateTime.now())
                            .clearanceStatus("ALREADY_CLOSED")
                            .message("Auction was already closed")
                            .build();
                }

                if (auction.getStatus() == AuctionStatus.CANCELLED) {
                    throw new InvalidAuctionStateException("Cannot clear a CANCELLED auction");
                }

                log.info("Initiating clearance for auction ID: {}", id);
                auction.setStatus(AuctionStatus.CLOSED);

                ClearanceResultDto clearanceResult;
                try {
                    clearanceResult = biddingClient.calculateClearance(id);
                } catch (Exception e) {
                    log.warn("Could not fetch clearance from Bidding Service for auction {}: {}", id, e.getMessage());
                    BigDecimal reserve = auction.getReservePrice() != null ? auction.getReservePrice() : auction.getStartingPrice();
                    clearanceResult = ClearanceResultDto.builder()
                            .auctionId(id)
                            .winningBidderId(auction.getWinnerId())
                            .winningAmount(auction.getCurrentPrice())
                            .reserveMet(auction.getCurrentPrice() != null && auction.getCurrentPrice().compareTo(reserve) >= 0)
                            .clearedAt(LocalDateTime.now())
                            .clearanceStatus("CLEARED_LOCAL")
                            .message("Local clearance fallback")
                            .build();
                }

                if (clearanceResult != null && clearanceResult.getWinningBidderId() != null && clearanceResult.isReserveMet()) {
                    auction.setWinnerId(clearanceResult.getWinningBidderId());
                    auction.setCurrentPrice(clearanceResult.getWinningAmount());

                    try {
                        PaymentSettlementRequest settlementRequest = PaymentSettlementRequest.builder()
                                .auctionId(id)
                                .sellerId(auction.getSellerId())
                                .buyerId(clearanceResult.getWinningBidderId())
                                .amount(clearanceResult.getWinningAmount())
                                .build();

                        PaymentSettlementResponse paymentResponse = paymentClient.processSettlement(settlementRequest);
                        log.info("Auction ID {} settled successfully with payment tx: {}", id, paymentResponse.getTransactionId());
                    } catch (Exception e) {
                        log.error("Payment settlement failed for auction ID {}: {}", id, e.getMessage());
                    }
                } else {
                    log.info("Auction ID {} closed without winning bid (reserve not met or no bids)", id);
                }

                auctionRepository.save(auction);
                return clearanceResult;
            });
        } finally {
            lock.unlock();
        }
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
        if ("ROLE_ADMIN".equalsIgnoreCase(userRole) || "ADMIN".equalsIgnoreCase(userRole)) {
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
                .currentPrice(a.getCurrentPrice())
                .minimumIncrement(a.getMinimumIncrement())
                .reservePrice(a.getReservePrice())
                .winnerId(a.getWinnerId())
                .sellerId(a.getSellerId())
                .status(a.getStatus())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .version(a.getVersion())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}

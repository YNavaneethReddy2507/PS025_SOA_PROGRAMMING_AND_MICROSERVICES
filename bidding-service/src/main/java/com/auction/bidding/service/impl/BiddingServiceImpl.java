package com.auction.bidding.service.impl;

import com.auction.bidding.client.AuctionClient;
import com.auction.bidding.dto.*;
import com.auction.bidding.entity.AuctionBidState;
import com.auction.bidding.entity.Bid;
import com.auction.bidding.entity.BidStatus;
import com.auction.bidding.exception.*;
import com.auction.bidding.repository.AuctionBidStateRepository;
import com.auction.bidding.repository.BidRepository;
import com.auction.bidding.service.BiddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
public class BiddingServiceImpl implements BiddingService {

    private static final Logger log = LoggerFactory.getLogger(BiddingServiceImpl.class);

    private final BidRepository bidRepository;
    private final AuctionBidStateRepository stateRepository;
    private final AuctionClient auctionClient;

    // Per-auction mutex lock to serialize simultaneous concurrent bids
    private final ConcurrentHashMap<Long, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

    public BiddingServiceImpl(BidRepository bidRepository, AuctionBidStateRepository stateRepository, AuctionClient auctionClient) {
        this.bidRepository = bidRepository;
        this.stateRepository = stateRepository;
        this.auctionClient = auctionClient;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BidResponse placeBid(PlaceBidRequest request, Long bidderId) {
        Long auctionId = request.getAuctionId();
        BigDecimal bidAmount = request.getAmount();
        LocalDateTime bidTime = LocalDateTime.now();

        if (bidderId == null) {
            throw new UnauthorizedException("Bidder identity is missing or invalid");
        }
        if (bidAmount == null || bidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Bid amount must be greater than 0");
        }

        log.info("Received bid placement request: bidder={}, auction={}, amount={}", bidderId, auctionId, bidAmount);

        ReentrantLock lock = auctionLocks.computeIfAbsent(auctionId, k -> new ReentrantLock(true));
        lock.lock();
        try {
            // 1. Fetch live auction metadata from Auction Service
            AuctionDto auctionDto = auctionClient.getAuctionById(auctionId);
            if (auctionDto == null) {
                log.warn("Bid rejected: Auction {} does not exist", auctionId);
                throw new ResourceNotFoundException("Auction not found with ID: " + auctionId);
            }

            // 2. Validate auction is ACTIVE
            if (!"ACTIVE".equalsIgnoreCase(auctionDto.getStatus())) {
                log.warn("Bid rejected: Auction {} is not ACTIVE (current: {})", auctionId, auctionDto.getStatus());
                throw new AuctionNotActiveException("Auction is not in ACTIVE state. Current status: " + auctionDto.getStatus());
            }

            // 3. Validate current time is before endTime
            if (auctionDto.getEndTime() != null && LocalDateTime.now().isAfter(auctionDto.getEndTime())) {
                log.warn("Bid rejected: Auction {} bidding period has expired (endTime={})", auctionId, auctionDto.getEndTime());
                throw new AuctionNotActiveException("Auction bidding period has expired");
            }

            // 4. Validate bidder is not the seller
            if (auctionDto.getSellerId() != null && auctionDto.getSellerId().equals(bidderId)) {
                log.warn("Bid rejected: Seller {} cannot bid on own auction {}", bidderId, auctionId);
                throw new SellerCannotBidException("Bidder cannot be seller");
            }

            // 5. Fetch or sync AuctionBidState
            AuctionBidState state = getOrSyncAuctionBidState(auctionId, auctionDto);

            // 6. Validate bid amount exceeds current highest bid and satisfies minimum increment
            BigDecimal currentHighest = state.getCurrentHighestBid();
            BigDecimal minInc = state.getMinBidIncrement() != null ? state.getMinBidIncrement() : BigDecimal.ONE;

            BigDecimal minRequiredBid;
            if (state.getWinningBidderId() == null) {
                // First bid must be at least startingPrice
                minRequiredBid = currentHighest;
            } else {
                // Subsequent bids must exceed currentHighest by at least minBidIncrement
                minRequiredBid = currentHighest.add(minInc);
            }

            if (bidAmount.compareTo(minRequiredBid) < 0) {
                log.warn("Sub-threshold bid rejected for auction {}: Offered {}, required at least {}",
                        auctionId, bidAmount, minRequiredBid);
                throw new SubThresholdBidException(String.format(
                        "Bid amount %s is below minimum required bid threshold %s", bidAmount, minRequiredBid
                ));
            }

            // 7. Persist accepted bid
            Bid newBid = Bid.builder()
                    .auctionId(auctionId)
                    .bidderId(bidderId)
                    .amount(bidAmount)
                    .status(BidStatus.ACCEPTED)
                    .acceptedAt(bidTime)
                    .build();

            Bid savedBid = bidRepository.save(newBid);

            // 8. Update high-concurrency state atomically
            state.setCurrentHighestBid(bidAmount);
            state.setWinningBidderId(bidderId);
            state.setTotalBidsCount(state.getTotalBidsCount() + 1);
            stateRepository.save(state);

            // 9. Propagate highest bid to Auction Service
            try {
                auctionClient.updateHighestBid(auctionId, UpdateHighestBidRequest.builder()
                        .bidderId(bidderId)
                        .amount(bidAmount)
                        .build());
            } catch (Exception e) {
                log.warn("Could not sync highest bid to Auction Service for auction {}: {}", auctionId, e.getMessage());
            }

            log.info("Bid successfully accepted and persisted: bidId={}, auctionId={}, amount={}", savedBid.getId(), auctionId, bidAmount);

            return mapToResponse(savedBid, "Bid accepted successfully", true);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BidResponse getBidById(Long id) {
        Bid bid = bidRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bid not found with ID: " + id));
        return mapToResponse(bid, "Bid retrieved successfully", false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BidResponse> getBidsByAuction(Long auctionId) {
        return bidRepository.findByAuctionIdOrderByAcceptedAtDesc(auctionId)
                .stream()
                .map(b -> mapToResponse(b, "Bid record", b.getStatus() == BidStatus.ACCEPTED))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BidResponse getHighestBid(Long auctionId) {
        Bid highestBid = bidRepository.findHighestAcceptedBid(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("No accepted bids found for auction: " + auctionId));

        return mapToResponse(highestBid, "Current highest bid", true);
    }

    @Override
    @Transactional(readOnly = true)
    public ClearanceResultDto calculateClearance(Long auctionId) {
        log.info("Calculating deterministic clearance for auction ID: {}", auctionId);

        List<Bid> bids = bidRepository.findByAuctionIdOrderByAmountDescAcceptedAtAsc(auctionId);

        AuctionBidState state = stateRepository.findByAuctionId(auctionId)
                .orElseGet(() -> {
                    AuctionDto dto = auctionClient.getAuctionById(auctionId);
                    return getOrSyncAuctionBidState(auctionId, dto);
                });

        if (bids.isEmpty()) {
            log.info("Auction ID {} cleared with NO BIDS", auctionId);
            return ClearanceResultDto.builder()
                    .auctionId(auctionId)
                    .winningBidderId(null)
                    .winningAmount(BigDecimal.ZERO)
                    .reserveMet(false)
                    .clearedAt(LocalDateTime.now())
                    .clearanceStatus("NO_BIDS")
                    .message("No bids were placed on this auction")
                    .build();
        }

        Bid winningBid = bids.get(0);
        BigDecimal reserve = state.getReservePrice() != null ? state.getReservePrice() : state.getCurrentHighestBid();
        boolean reserveMet = winningBid.getAmount().compareTo(reserve) >= 0;

        String clearanceStatus = reserveMet ? "CLEARED" : "RESERVE_NOT_MET";
        String message = reserveMet ?
                "Auction successfully cleared with winner" :
                "Highest bid did not meet the seller's reserve price";

        log.info("Auction clearance calculated: auctionId={}, winner={}, amount={}, reserveMet={}",
                auctionId, winningBid.getBidderId(), winningBid.getAmount(), reserveMet);

        return ClearanceResultDto.builder()
                .auctionId(auctionId)
                .winningBidderId(reserveMet ? winningBid.getBidderId() : null)
                .winningAmount(winningBid.getAmount())
                .reserveMet(reserveMet)
                .clearedAt(LocalDateTime.now())
                .clearanceStatus(clearanceStatus)
                .message(message)
                .build();
    }

    private AuctionBidState getOrSyncAuctionBidState(Long auctionId, AuctionDto auctionDto) {
        return stateRepository.findByAuctionId(auctionId).orElseGet(() -> {
            if (auctionDto == null) {
                throw new ResourceNotFoundException("Auction not found with ID: " + auctionId);
            }

            BigDecimal currentPrice = auctionDto.getCurrentPrice() != null ?
                    auctionDto.getCurrentPrice() : auctionDto.getStartingPrice();

            BigDecimal minInc = auctionDto.getMinimumIncrement() != null ?
                    auctionDto.getMinimumIncrement() : BigDecimal.ONE;

            BigDecimal reserve = auctionDto.getReservePrice() != null ?
                    auctionDto.getReservePrice() : currentPrice;

            AuctionBidState newState = AuctionBidState.builder()
                    .auctionId(auctionId)
                    .currentHighestBid(currentPrice)
                    .winningBidderId(auctionDto.getWinnerId())
                    .minBidIncrement(minInc)
                    .reservePrice(reserve)
                    .sellerId(auctionDto.getSellerId())
                    .totalBidsCount(0L)
                    .build();

            return stateRepository.save(newState);
        });
    }

    private BidResponse mapToResponse(Bid bid, String message, boolean isWinning) {
        return BidResponse.builder()
                .id(bid.getId())
                .auctionId(bid.getAuctionId())
                .bidderId(bid.getBidderId())
                .amount(bid.getAmount())
                .status(bid.getStatus())
                .acceptedAt(bid.getAcceptedAt())
                .message(message)
                .isWinning(isWinning)
                .build();
    }
}

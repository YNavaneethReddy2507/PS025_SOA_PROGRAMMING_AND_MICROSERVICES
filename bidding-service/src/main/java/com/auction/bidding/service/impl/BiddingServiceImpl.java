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

    // Fine-grained lock per auction to protect critical bidding sections under high concurrency
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
        BigDecimal bidAmount = request.getBidAmount();
        LocalDateTime bidTime = LocalDateTime.now();

        log.info("Received bid placement request: bidder={}, auction={}, amount={}", bidderId, auctionId, bidAmount);

        ReentrantLock lock = auctionLocks.computeIfAbsent(auctionId, k -> new ReentrantLock());
        lock.lock();
        try {
            // 1. Fetch current auction details via Feign client or local state cache
            AuctionBidState state = getOrSyncAuctionBidState(auctionId);

            // 2. Validate bidder is not the seller
            if (state.getSellerId().equals(bidderId)) {
                log.warn("Bid rejected: Seller {} cannot bid on their own auction {}", bidderId, auctionId);
                throw new SellerCannotBidException("Sellers cannot place bids on their own auctions");
            }

            // 3. Validate bid amount against starting price or current highest bid + min increment
            BigDecimal minRequiredBid;
            if (state.getWinningBidderId() == null) {
                minRequiredBid = state.getCurrentHighestBid();
            } else {
                minRequiredBid = state.getCurrentHighestBid().add(state.getMinBidIncrement());
            }

            if (bidAmount.compareTo(minRequiredBid) < 0) {
                log.warn("Sub-threshold bid rejected for auction {}: Offered {}, required at least {}", auctionId, bidAmount, minRequiredBid);
                throw new SubThresholdBidException(String.format(
                        "Bid amount %s is below minimum required bid threshold %s", bidAmount, minRequiredBid
                ));
            }

            // 4. Mark previous winning bids as OUTBID
            bidRepository.updateBidStatusForAuction(auctionId, BidStatus.WINNING, BidStatus.OUTBID);

            // 5. Create new winning bid record
            Bid newBid = Bid.builder()
                    .auctionId(auctionId)
                    .bidderId(bidderId)
                    .bidAmount(bidAmount)
                    .status(BidStatus.WINNING)
                    .bidTimestamp(bidTime)
                    .build();

            Bid savedBid = bidRepository.save(newBid);

            // 6. Update high-concurrency state
            state.setCurrentHighestBid(bidAmount);
            state.setWinningBidderId(bidderId);
            state.setTotalBidsCount(state.getTotalBidsCount() + 1);
            stateRepository.save(state);

            // 7. Propagate highest bid to Auction Service asynchronously or synchronously
            try {
                auctionClient.updateHighestBid(auctionId, UpdateHighestBidRequest.builder()
                        .bidderId(bidderId)
                        .amount(bidAmount)
                        .build());
            } catch (Exception e) {
                log.warn("Could not sync highest bid to Auction Service for auction {}: {}", auctionId, e.getMessage());
            }

            log.info("Bid successfully accepted: bidId={}, auctionId={}, amount={}", savedBid.getId(), auctionId, bidAmount);

            return mapToResponse(savedBid, true);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BidResponse> getBidsByAuction(Long auctionId) {
        return bidRepository.findByAuctionIdOrderByBidTimestampDesc(auctionId)
                .stream()
                .map(b -> mapToResponse(b, b.getStatus() == BidStatus.WINNING))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ClearanceResultDto calculateClearance(Long auctionId) {
        log.info("Calculating deterministic clearance for auction ID: {}", auctionId);

        // Fetch all bids ordered deterministically: Highest amount first, earliest timestamp as tie-breaker
        List<Bid> bids = bidRepository.findByAuctionIdOrderByBidAmountDescBidTimestampAsc(auctionId);

        AuctionBidState state = stateRepository.findByAuctionId(auctionId)
                .orElseGet(() -> getOrSyncAuctionBidState(auctionId));

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
        boolean reserveMet = winningBid.getBidAmount().compareTo(state.getReservePrice()) >= 0;

        String clearanceStatus = reserveMet ? "CLEARED" : "RESERVE_NOT_MET";
        String message = reserveMet ?
                "Auction successfully cleared with winner" :
                "Highest bid did not meet the seller's reserve price";

        log.info("Auction clearance calculated: auctionId={}, winner={}, amount={}, reserveMet={}",
                auctionId, winningBid.getBidderId(), winningBid.getBidAmount(), reserveMet);

        return ClearanceResultDto.builder()
                .auctionId(auctionId)
                .winningBidderId(reserveMet ? winningBid.getBidderId() : null)
                .winningAmount(winningBid.getBidAmount())
                .reserveMet(reserveMet)
                .clearedAt(LocalDateTime.now())
                .clearanceStatus(clearanceStatus)
                .message(message)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BidResponse getHighestBid(Long auctionId) {
        Bid highestBid = bidRepository.findFirstByAuctionIdAndStatus(auctionId, BidStatus.WINNING)
                .orElseThrow(() -> new ResourceNotFoundException("No winning bids found for auction: " + auctionId));

        return mapToResponse(highestBid, true);
    }

    private AuctionBidState getOrSyncAuctionBidState(Long auctionId) {
        return stateRepository.findByAuctionId(auctionId).orElseGet(() -> {
            log.info("Synchronizing auction state from Auction Service for ID: {}", auctionId);
            AuctionDto auctionDto = auctionClient.getAuctionById(auctionId);
            if (auctionDto == null) {
                throw new ResourceNotFoundException("Auction not found with ID: " + auctionId);
            }

            if (!"ACTIVE".equalsIgnoreCase(auctionDto.getStatus())) {
                throw new AuctionNotActiveException("Auction is not in ACTIVE state. Current: " + auctionDto.getStatus());
            }

            if (auctionDto.getEndTime() != null && LocalDateTime.now().isAfter(auctionDto.getEndTime())) {
                throw new AuctionNotActiveException("Auction bidding period has expired");
            }

            AuctionBidState newState = AuctionBidState.builder()
                    .auctionId(auctionId)
                    .currentHighestBid(auctionDto.getCurrentHighestBid() != null ? auctionDto.getCurrentHighestBid() : auctionDto.getStartingPrice())
                    .winningBidderId(auctionDto.getWinningBidderId())
                    .minBidIncrement(auctionDto.getMinBidIncrement())
                    .reservePrice(auctionDto.getReservePrice())
                    .sellerId(auctionDto.getSellerId())
                    .totalBidsCount(0L)
                    .build();

            return stateRepository.save(newState);
        });
    }

    private BidResponse mapToResponse(Bid bid, boolean isWinning) {
        return BidResponse.builder()
                .id(bid.getId())
                .auctionId(bid.getAuctionId())
                .bidderId(bid.getBidderId())
                .bidAmount(bid.getBidAmount())
                .status(bid.getStatus())
                .bidTimestamp(bid.getBidTimestamp())
                .isWinning(isWinning)
                .build();
    }
}

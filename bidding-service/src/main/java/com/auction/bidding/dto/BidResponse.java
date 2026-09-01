package com.auction.bidding.dto;

import com.auction.bidding.entity.BidStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BidResponse {

    private Long id;
    private Long auctionId;
    private Long bidderId;
    private BigDecimal bidAmount;
    private BidStatus status;
    private LocalDateTime bidTimestamp;
    private boolean isWinning;

    public BidResponse() {
    }

    public BidResponse(Long id, Long auctionId, Long bidderId, BigDecimal bidAmount, BidStatus status, LocalDateTime bidTimestamp, boolean isWinning) {
        this.id = id;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.status = status;
        this.bidTimestamp = bidTimestamp;
        this.isWinning = isWinning;
    }

    public static BidResponseBuilder builder() {
        return new BidResponseBuilder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
    }

    public Long getBidderId() {
        return bidderId;
    }

    public void setBidderId(Long bidderId) {
        this.bidderId = bidderId;
    }

    public BigDecimal getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(BigDecimal bidAmount) {
        this.bidAmount = bidAmount;
    }

    public BidStatus getStatus() {
        return status;
    }

    public void setStatus(BidStatus status) {
        this.status = status;
    }

    public LocalDateTime getBidTimestamp() {
        return bidTimestamp;
    }

    public void setBidTimestamp(LocalDateTime bidTimestamp) {
        this.bidTimestamp = bidTimestamp;
    }

    public boolean isWinning() {
        return isWinning;
    }

    public void setWinning(boolean winning) {
        isWinning = winning;
    }

    public static class BidResponseBuilder {
        private Long id;
        private Long auctionId;
        private Long bidderId;
        private BigDecimal bidAmount;
        private BidStatus status;
        private LocalDateTime bidTimestamp;
        private boolean isWinning;

        BidResponseBuilder() {
        }

        public BidResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public BidResponseBuilder auctionId(Long auctionId) {
            this.auctionId = auctionId;
            return this;
        }

        public BidResponseBuilder bidderId(Long bidderId) {
            this.bidderId = bidderId;
            return this;
        }

        public BidResponseBuilder bidAmount(BigDecimal bidAmount) {
            this.bidAmount = bidAmount;
            return this;
        }

        public BidResponseBuilder status(BidStatus status) {
            this.status = status;
            return this;
        }

        public BidResponseBuilder bidTimestamp(LocalDateTime bidTimestamp) {
            this.bidTimestamp = bidTimestamp;
            return this;
        }

        public BidResponseBuilder isWinning(boolean isWinning) {
            this.isWinning = isWinning;
            return this;
        }

        public BidResponse build() {
            return new BidResponse(id, auctionId, bidderId, bidAmount, status, bidTimestamp, isWinning);
        }
    }
}

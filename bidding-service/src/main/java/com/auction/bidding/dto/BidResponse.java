package com.auction.bidding.dto;

import com.auction.bidding.entity.BidStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BidResponse {

    private Long id;
    private Long auctionId;
    private Long bidderId;
    private BigDecimal amount;
    private BidStatus status;
    private LocalDateTime acceptedAt;
    private String message;
    private boolean isWinning;

    public BidResponse() {
    }

    public BidResponse(Long id, Long auctionId, Long bidderId, BigDecimal amount, BidStatus status, LocalDateTime acceptedAt, String message, boolean isWinning) {
        this.id = id;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.status = status;
        this.acceptedAt = acceptedAt;
        this.message = message;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BidStatus getStatus() {
        return status;
    }

    public void setStatus(BidStatus status) {
        this.status = status;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isWinning() {
        return isWinning;
    }

    public void setWinning(boolean winning) {
        isWinning = winning;
    }

    // --- Aliases for backward compatibility ---
    public BigDecimal getBidAmount() {
        return amount;
    }

    public void setBidAmount(BigDecimal bidAmount) {
        this.amount = bidAmount;
    }

    public LocalDateTime getBidTimestamp() {
        return acceptedAt;
    }

    public void setBidTimestamp(LocalDateTime bidTimestamp) {
        this.acceptedAt = bidTimestamp;
    }

    public static class BidResponseBuilder {
        private Long id;
        private Long auctionId;
        private Long bidderId;
        private BigDecimal amount;
        private BidStatus status;
        private LocalDateTime acceptedAt;
        private String message;
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

        public BidResponseBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public BidResponseBuilder bidAmount(BigDecimal bidAmount) {
            this.amount = bidAmount;
            return this;
        }

        public BidResponseBuilder status(BidStatus status) {
            this.status = status;
            return this;
        }

        public BidResponseBuilder acceptedAt(LocalDateTime acceptedAt) {
            this.acceptedAt = acceptedAt;
            return this;
        }

        public BidResponseBuilder bidTimestamp(LocalDateTime bidTimestamp) {
            this.acceptedAt = bidTimestamp;
            return this;
        }

        public BidResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public BidResponseBuilder isWinning(boolean isWinning) {
            this.isWinning = isWinning;
            return this;
        }

        public BidResponse build() {
            return new BidResponse(id, auctionId, bidderId, amount, status, acceptedAt, message, isWinning);
        }
    }
}

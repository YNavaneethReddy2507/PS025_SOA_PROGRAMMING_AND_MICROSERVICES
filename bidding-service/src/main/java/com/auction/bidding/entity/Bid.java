package com.auction.bidding.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "bids",
        indexes = {
                @Index(name = "idx_auction_amount_time", columnList = "auction_id, bid_amount, bid_timestamp"),
                @Index(name = "idx_bidder_id", columnList = "bidder_id")
        }
)
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auction_id", nullable = false)
    private Long auctionId;

    @Column(name = "bidder_id", nullable = false)
    private Long bidderId;

    @Column(name = "bid_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal bidAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BidStatus status;

    @Column(name = "bid_timestamp", nullable = false)
    private LocalDateTime bidTimestamp;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Bid() {
    }

    public Bid(Long id, Long auctionId, Long bidderId, BigDecimal bidAmount, BidStatus status, LocalDateTime bidTimestamp, LocalDateTime createdAt) {
        this.id = id;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.status = status;
        this.bidTimestamp = bidTimestamp;
        this.createdAt = createdAt;
    }

    public static BidBuilder builder() {
        return new BidBuilder();
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static class BidBuilder {
        private Long id;
        private Long auctionId;
        private Long bidderId;
        private BigDecimal bidAmount;
        private BidStatus status;
        private LocalDateTime bidTimestamp;
        private LocalDateTime createdAt;

        BidBuilder() {
        }

        public BidBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public BidBuilder auctionId(Long auctionId) {
            this.auctionId = auctionId;
            return this;
        }

        public BidBuilder bidderId(Long bidderId) {
            this.bidderId = bidderId;
            return this;
        }

        public BidBuilder bidAmount(BigDecimal bidAmount) {
            this.bidAmount = bidAmount;
            return this;
        }

        public BidBuilder status(BidStatus status) {
            this.status = status;
            return this;
        }

        public BidBuilder bidTimestamp(LocalDateTime bidTimestamp) {
            this.bidTimestamp = bidTimestamp;
            return this;
        }

        public BidBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Bid build() {
            return new Bid(id, auctionId, bidderId, bidAmount, status, bidTimestamp, createdAt);
        }
    }
}

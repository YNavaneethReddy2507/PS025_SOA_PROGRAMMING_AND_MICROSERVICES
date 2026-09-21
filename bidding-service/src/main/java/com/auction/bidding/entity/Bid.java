package com.auction.bidding.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "bids",
        indexes = {
                @Index(name = "idx_auction_amount_time", columnList = "auction_id, amount, accepted_at"),
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

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BidStatus status;

    @Column(name = "accepted_at", nullable = false)
    private LocalDateTime acceptedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Bid() {
    }

    public Bid(Long id, Long auctionId, Long bidderId, BigDecimal amount, BidStatus status, LocalDateTime acceptedAt, LocalDateTime createdAt) {
        this.id = id;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.status = status;
        this.acceptedAt = acceptedAt;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    public static class BidBuilder {
        private Long id;
        private Long auctionId;
        private Long bidderId;
        private BigDecimal amount;
        private BidStatus status;
        private LocalDateTime acceptedAt;
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

        public BidBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public BidBuilder bidAmount(BigDecimal bidAmount) {
            this.amount = bidAmount;
            return this;
        }

        public BidBuilder status(BidStatus status) {
            this.status = status;
            return this;
        }

        public BidBuilder acceptedAt(LocalDateTime acceptedAt) {
            this.acceptedAt = acceptedAt;
            return this;
        }

        public BidBuilder bidTimestamp(LocalDateTime bidTimestamp) {
            this.acceptedAt = bidTimestamp;
            return this;
        }

        public BidBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Bid build() {
            LocalDateTime timestamp = this.acceptedAt != null ? this.acceptedAt : LocalDateTime.now();
            return new Bid(id, auctionId, bidderId, amount, status, timestamp, createdAt);
        }
    }
}

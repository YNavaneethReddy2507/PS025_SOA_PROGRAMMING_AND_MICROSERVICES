package com.auction.auction.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "auctions")
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 64)
    private String category;

    @Column(name = "starting_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal startingPrice;

    @Column(name = "reserve_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal reservePrice;

    @Column(name = "min_bid_increment", nullable = false, precision = 19, scale = 4)
    private BigDecimal minBidIncrement;

    @Column(name = "current_highest_bid", precision = 19, scale = 4)
    private BigDecimal currentHighestBid;

    @Column(name = "winning_bidder_id")
    private Long winningBidderId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuctionStatus status;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Auction() {
    }

    public Auction(Long id, String title, String description, String category, BigDecimal startingPrice,
                   BigDecimal reservePrice, BigDecimal minBidIncrement, BigDecimal currentHighestBid,
                   Long winningBidderId, Long sellerId, AuctionStatus status, LocalDateTime startTime,
                   LocalDateTime endTime, Long version, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.startingPrice = startingPrice;
        this.reservePrice = reservePrice;
        this.minBidIncrement = minBidIncrement;
        this.currentHighestBid = currentHighestBid;
        this.winningBidderId = winningBidderId;
        this.sellerId = sellerId;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static AuctionBuilder builder() {
        return new AuctionBuilder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(BigDecimal startingPrice) {
        this.startingPrice = startingPrice;
    }

    public BigDecimal getReservePrice() {
        return reservePrice;
    }

    public void setReservePrice(BigDecimal reservePrice) {
        this.reservePrice = reservePrice;
    }

    public BigDecimal getMinBidIncrement() {
        return minBidIncrement;
    }

    public void setMinBidIncrement(BigDecimal minBidIncrement) {
        this.minBidIncrement = minBidIncrement;
    }

    public BigDecimal getCurrentHighestBid() {
        return currentHighestBid;
    }

    public void setCurrentHighestBid(BigDecimal currentHighestBid) {
        this.currentHighestBid = currentHighestBid;
    }

    public Long getWinningBidderId() {
        return winningBidderId;
    }

    public void setWinningBidderId(Long winningBidderId) {
        this.winningBidderId = winningBidderId;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static class AuctionBuilder {
        private Long id;
        private String title;
        private String description;
        private String category;
        private BigDecimal startingPrice;
        private BigDecimal reservePrice;
        private BigDecimal minBidIncrement;
        private BigDecimal currentHighestBid;
        private Long winningBidderId;
        private Long sellerId;
        private AuctionStatus status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Long version;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        AuctionBuilder() {
        }

        public AuctionBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public AuctionBuilder title(String title) {
            this.title = title;
            return this;
        }

        public AuctionBuilder description(String description) {
            this.description = description;
            return this;
        }

        public AuctionBuilder category(String category) {
            this.category = category;
            return this;
        }

        public AuctionBuilder startingPrice(BigDecimal startingPrice) {
            this.startingPrice = startingPrice;
            return this;
        }

        public AuctionBuilder reservePrice(BigDecimal reservePrice) {
            this.reservePrice = reservePrice;
            return this;
        }

        public AuctionBuilder minBidIncrement(BigDecimal minBidIncrement) {
            this.minBidIncrement = minBidIncrement;
            return this;
        }

        public AuctionBuilder currentHighestBid(BigDecimal currentHighestBid) {
            this.currentHighestBid = currentHighestBid;
            return this;
        }

        public AuctionBuilder winningBidderId(Long winningBidderId) {
            this.winningBidderId = winningBidderId;
            return this;
        }

        public AuctionBuilder sellerId(Long sellerId) {
            this.sellerId = sellerId;
            return this;
        }

        public AuctionBuilder status(AuctionStatus status) {
            this.status = status;
            return this;
        }

        public AuctionBuilder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public AuctionBuilder endTime(LocalDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public AuctionBuilder version(Long version) {
            this.version = version;
            return this;
        }

        public AuctionBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public AuctionBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Auction build() {
            return new Auction(id, title, description, category, startingPrice, reservePrice, minBidIncrement,
                    currentHighestBid, winningBidderId, sellerId, status, startTime, endTime, version, createdAt, updatedAt);
        }
    }
}

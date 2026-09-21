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

    @Column(length = 64)
    private String category;

    @Column(name = "starting_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal startingPrice;

    @Column(name = "current_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentPrice;

    @Column(name = "minimum_increment", nullable = false, precision = 19, scale = 4)
    private BigDecimal minimumIncrement;

    @Column(name = "reserve_price", precision = 19, scale = 4)
    private BigDecimal reservePrice;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuctionStatus status;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "winner_id")
    private Long winnerId;

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
                   BigDecimal currentPrice, BigDecimal minimumIncrement, BigDecimal reservePrice,
                   LocalDateTime startTime, LocalDateTime endTime, AuctionStatus status,
                   Long sellerId, Long winnerId, Long version,
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.startingPrice = startingPrice;
        this.currentPrice = currentPrice;
        this.minimumIncrement = minimumIncrement;
        this.reservePrice = reservePrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.sellerId = sellerId;
        this.winnerId = winnerId;
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

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getMinimumIncrement() {
        return minimumIncrement;
    }

    public void setMinimumIncrement(BigDecimal minimumIncrement) {
        this.minimumIncrement = minimumIncrement;
    }

    public BigDecimal getReservePrice() {
        return reservePrice;
    }

    public void setReservePrice(BigDecimal reservePrice) {
        this.reservePrice = reservePrice;
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

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public Long getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(Long winnerId) {
        this.winnerId = winnerId;
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

    // --- Aliases for compatibility ---
    public BigDecimal getCurrentHighestBid() {
        return currentPrice;
    }

    public void setCurrentHighestBid(BigDecimal currentHighestBid) {
        this.currentPrice = currentHighestBid;
    }

    public BigDecimal getMinBidIncrement() {
        return minimumIncrement;
    }

    public void setMinBidIncrement(BigDecimal minBidIncrement) {
        this.minimumIncrement = minBidIncrement;
    }

    public Long getWinningBidderId() {
        return winnerId;
    }

    public void setWinningBidderId(Long winningBidderId) {
        this.winnerId = winningBidderId;
    }

    public static class AuctionBuilder {
        private Long id;
        private String title;
        private String description;
        private String category = "GENERAL";
        private BigDecimal startingPrice;
        private BigDecimal currentPrice;
        private BigDecimal minimumIncrement;
        private BigDecimal reservePrice;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private AuctionStatus status;
        private Long sellerId;
        private Long winnerId;
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

        public AuctionBuilder currentPrice(BigDecimal currentPrice) {
            this.currentPrice = currentPrice;
            return this;
        }

        public AuctionBuilder currentHighestBid(BigDecimal currentHighestBid) {
            this.currentPrice = currentHighestBid;
            return this;
        }

        public AuctionBuilder minimumIncrement(BigDecimal minimumIncrement) {
            this.minimumIncrement = minimumIncrement;
            return this;
        }

        public AuctionBuilder minBidIncrement(BigDecimal minBidIncrement) {
            this.minimumIncrement = minBidIncrement;
            return this;
        }

        public AuctionBuilder reservePrice(BigDecimal reservePrice) {
            this.reservePrice = reservePrice;
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

        public AuctionBuilder status(AuctionStatus status) {
            this.status = status;
            return this;
        }

        public AuctionBuilder sellerId(Long sellerId) {
            this.sellerId = sellerId;
            return this;
        }

        public AuctionBuilder winnerId(Long winnerId) {
            this.winnerId = winnerId;
            return this;
        }

        public AuctionBuilder winningBidderId(Long winningBidderId) {
            this.winnerId = winningBidderId;
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
            BigDecimal initialCurrentPrice = this.currentPrice != null ? this.currentPrice : this.startingPrice;
            BigDecimal initialReserve = this.reservePrice != null ? this.reservePrice : this.startingPrice;
            return new Auction(id, title, description, category, startingPrice, initialCurrentPrice,
                    minimumIncrement, initialReserve, startTime, endTime, status, sellerId, winnerId, version, createdAt, updatedAt);
        }
    }
}

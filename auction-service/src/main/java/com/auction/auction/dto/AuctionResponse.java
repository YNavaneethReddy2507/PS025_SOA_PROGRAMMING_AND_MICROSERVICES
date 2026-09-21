package com.auction.auction.dto;

import com.auction.auction.entity.AuctionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AuctionResponse {

    private Long id;
    private String title;
    private String description;
    private String category;
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

    public AuctionResponse() {
    }

    public AuctionResponse(Long id, String title, String description, String category, BigDecimal startingPrice,
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

    public static AuctionResponseBuilder builder() {
        return new AuctionResponseBuilder();
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

    public static class AuctionResponseBuilder {
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

        AuctionResponseBuilder() {
        }

        public AuctionResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public AuctionResponseBuilder title(String title) {
            this.title = title;
            return this;
        }

        public AuctionResponseBuilder description(String description) {
            this.description = description;
            return this;
        }

        public AuctionResponseBuilder category(String category) {
            this.category = category;
            return this;
        }

        public AuctionResponseBuilder startingPrice(BigDecimal startingPrice) {
            this.startingPrice = startingPrice;
            return this;
        }

        public AuctionResponseBuilder currentPrice(BigDecimal currentPrice) {
            this.currentPrice = currentPrice;
            return this;
        }

        public AuctionResponseBuilder currentHighestBid(BigDecimal currentHighestBid) {
            this.currentPrice = currentHighestBid;
            return this;
        }

        public AuctionResponseBuilder minimumIncrement(BigDecimal minimumIncrement) {
            this.minimumIncrement = minimumIncrement;
            return this;
        }

        public AuctionResponseBuilder minBidIncrement(BigDecimal minBidIncrement) {
            this.minimumIncrement = minBidIncrement;
            return this;
        }

        public AuctionResponseBuilder reservePrice(BigDecimal reservePrice) {
            this.reservePrice = reservePrice;
            return this;
        }

        public AuctionResponseBuilder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public AuctionResponseBuilder endTime(LocalDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public AuctionResponseBuilder status(AuctionStatus status) {
            this.status = status;
            return this;
        }

        public AuctionResponseBuilder sellerId(Long sellerId) {
            this.sellerId = sellerId;
            return this;
        }

        public AuctionResponseBuilder winnerId(Long winnerId) {
            this.winnerId = winnerId;
            return this;
        }

        public AuctionResponseBuilder winningBidderId(Long winningBidderId) {
            this.winnerId = winningBidderId;
            return this;
        }

        public AuctionResponseBuilder version(Long version) {
            this.version = version;
            return this;
        }

        public AuctionResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public AuctionResponseBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public AuctionResponse build() {
            BigDecimal actualCurrentPrice = this.currentPrice != null ? this.currentPrice : this.startingPrice;
            BigDecimal actualReservePrice = this.reservePrice != null ? this.reservePrice : this.startingPrice;
            return new AuctionResponse(id, title, description, category, startingPrice, actualCurrentPrice,
                    minimumIncrement, actualReservePrice, startTime, endTime, status, sellerId, winnerId, version, createdAt, updatedAt);
        }
    }
}

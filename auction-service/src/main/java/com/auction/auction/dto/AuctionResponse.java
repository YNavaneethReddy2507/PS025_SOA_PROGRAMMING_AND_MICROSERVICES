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
    private BigDecimal reservePrice;
    private BigDecimal minBidIncrement;
    private BigDecimal currentHighestBid;
    private Long winningBidderId;
    private Long sellerId;
    private AuctionStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AuctionResponse() {
    }

    public AuctionResponse(Long id, String title, String description, String category, BigDecimal startingPrice,
                           BigDecimal reservePrice, BigDecimal minBidIncrement, BigDecimal currentHighestBid,
                           Long winningBidderId, Long sellerId, AuctionStatus status, LocalDateTime startTime,
                           LocalDateTime endTime, LocalDateTime createdAt, LocalDateTime updatedAt) {
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

    public static class AuctionResponseBuilder {
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

        public AuctionResponseBuilder reservePrice(BigDecimal reservePrice) {
            this.reservePrice = reservePrice;
            return this;
        }

        public AuctionResponseBuilder minBidIncrement(BigDecimal minBidIncrement) {
            this.minBidIncrement = minBidIncrement;
            return this;
        }

        public AuctionResponseBuilder currentHighestBid(BigDecimal currentHighestBid) {
            this.currentHighestBid = currentHighestBid;
            return this;
        }

        public AuctionResponseBuilder winningBidderId(Long winningBidderId) {
            this.winningBidderId = winningBidderId;
            return this;
        }

        public AuctionResponseBuilder sellerId(Long sellerId) {
            this.sellerId = sellerId;
            return this;
        }

        public AuctionResponseBuilder status(AuctionStatus status) {
            this.status = status;
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

        public AuctionResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public AuctionResponseBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public AuctionResponse build() {
            return new AuctionResponse(id, title, description, category, startingPrice, reservePrice,
                    minBidIncrement, currentHighestBid, winningBidderId, sellerId, status, startTime,
                    endTime, createdAt, updatedAt);
        }
    }
}

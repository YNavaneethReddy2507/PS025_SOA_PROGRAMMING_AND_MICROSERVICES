package com.auction.bidding.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AuctionDto {

    private Long id;
    private String title;
    private BigDecimal startingPrice;
    private BigDecimal reservePrice;
    private BigDecimal minBidIncrement;
    private BigDecimal currentHighestBid;
    private Long winningBidderId;
    private Long sellerId;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public AuctionDto() {
    }

    public AuctionDto(Long id, String title, BigDecimal startingPrice, BigDecimal reservePrice, BigDecimal minBidIncrement,
                      BigDecimal currentHighestBid, Long winningBidderId, Long sellerId, String status, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.title = title;
        this.startingPrice = startingPrice;
        this.reservePrice = reservePrice;
        this.minBidIncrement = minBidIncrement;
        this.currentHighestBid = currentHighestBid;
        this.winningBidderId = winningBidderId;
        this.sellerId = sellerId;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static AuctionDtoBuilder builder() {
        return new AuctionDtoBuilder();
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

    public static class AuctionDtoBuilder {
        private Long id;
        private String title;
        private BigDecimal startingPrice;
        private BigDecimal reservePrice;
        private BigDecimal minBidIncrement;
        private BigDecimal currentHighestBid;
        private Long winningBidderId;
        private Long sellerId;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        AuctionDtoBuilder() {
        }

        public AuctionDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public AuctionDtoBuilder title(String title) {
            this.title = title;
            return this;
        }

        public AuctionDtoBuilder startingPrice(BigDecimal startingPrice) {
            this.startingPrice = startingPrice;
            return this;
        }

        public AuctionDtoBuilder reservePrice(BigDecimal reservePrice) {
            this.reservePrice = reservePrice;
            return this;
        }

        public AuctionDtoBuilder minBidIncrement(BigDecimal minBidIncrement) {
            this.minBidIncrement = minBidIncrement;
            return this;
        }

        public AuctionDtoBuilder currentHighestBid(BigDecimal currentHighestBid) {
            this.currentHighestBid = currentHighestBid;
            return this;
        }

        public AuctionDtoBuilder winningBidderId(Long winningBidderId) {
            this.winningBidderId = winningBidderId;
            return this;
        }

        public AuctionDtoBuilder sellerId(Long sellerId) {
            this.sellerId = sellerId;
            return this;
        }

        public AuctionDtoBuilder status(String status) {
            this.status = status;
            return this;
        }

        public AuctionDtoBuilder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public AuctionDtoBuilder endTime(LocalDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public AuctionDto build() {
            return new AuctionDto(id, title, startingPrice, reservePrice, minBidIncrement, currentHighestBid, winningBidderId, sellerId, status, startTime, endTime);
        }
    }
}

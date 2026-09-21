package com.auction.payment.dto;

import java.math.BigDecimal;

public class AuctionDto {

    private Long id;
    private String title;
    private BigDecimal currentPrice;
    private BigDecimal startingPrice;
    private Long winnerId;
    private Long sellerId;
    private String status;

    public AuctionDto() {
    }

    public AuctionDto(Long id, String title, BigDecimal currentPrice, BigDecimal startingPrice,
                      Long winnerId, Long sellerId, String status) {
        this.id = id;
        this.title = title;
        this.currentPrice = currentPrice;
        this.startingPrice = startingPrice;
        this.winnerId = winnerId;
        this.sellerId = sellerId;
        this.status = status;
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

    public BigDecimal getCurrentPrice() {
        return currentPrice != null ? currentPrice : startingPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(BigDecimal startingPrice) {
        this.startingPrice = startingPrice;
    }

    public Long getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(Long winnerId) {
        this.winnerId = winnerId;
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

    public static class AuctionDtoBuilder {
        private Long id;
        private String title;
        private BigDecimal currentPrice;
        private BigDecimal startingPrice;
        private Long winnerId;
        private Long sellerId;
        private String status;

        public AuctionDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public AuctionDtoBuilder title(String title) {
            this.title = title;
            return this;
        }

        public AuctionDtoBuilder currentPrice(BigDecimal currentPrice) {
            this.currentPrice = currentPrice;
            return this;
        }

        public AuctionDtoBuilder startingPrice(BigDecimal startingPrice) {
            this.startingPrice = startingPrice;
            return this;
        }

        public AuctionDtoBuilder winnerId(Long winnerId) {
            this.winnerId = winnerId;
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

        public AuctionDto build() {
            return new AuctionDto(id, title, currentPrice, startingPrice, winnerId, sellerId, status);
        }
    }
}

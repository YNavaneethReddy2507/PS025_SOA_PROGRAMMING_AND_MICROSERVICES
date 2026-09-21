package com.auction.bidding.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class PlaceBidRequest {

    @NotNull(message = "Auction ID is required")
    private Long auctionId;

    @NotNull(message = "Bid amount is required")
    @DecimalMin(value = "0.01", message = "Bid amount must be greater than 0")
    private BigDecimal bidAmount;

    public PlaceBidRequest() {
    }

    public PlaceBidRequest(Long auctionId, BigDecimal bidAmount) {
        this.auctionId = auctionId;
        this.bidAmount = bidAmount;
    }

    public static PlaceBidRequestBuilder builder() {
        return new PlaceBidRequestBuilder();
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
    }

    public BigDecimal getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(BigDecimal bidAmount) {
        this.bidAmount = bidAmount;
    }

    public BigDecimal getAmount() {
        return bidAmount;
    }

    public void setAmount(BigDecimal amount) {
        this.bidAmount = amount;
    }

    public static class PlaceBidRequestBuilder {
        private Long auctionId;
        private BigDecimal bidAmount;

        PlaceBidRequestBuilder() {
        }

        public PlaceBidRequestBuilder auctionId(Long auctionId) {
            this.auctionId = auctionId;
            return this;
        }

        public PlaceBidRequestBuilder bidAmount(BigDecimal bidAmount) {
            this.bidAmount = bidAmount;
            return this;
        }

        public PlaceBidRequest build() {
            return new PlaceBidRequest(auctionId, bidAmount);
        }
    }
}

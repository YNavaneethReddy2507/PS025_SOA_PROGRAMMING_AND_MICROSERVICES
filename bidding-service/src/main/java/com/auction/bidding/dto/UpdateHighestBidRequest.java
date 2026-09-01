package com.auction.bidding.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class UpdateHighestBidRequest {

    @NotNull(message = "Bidder ID is required")
    private Long bidderId;

    @NotNull(message = "Bid amount is required")
    @DecimalMin(value = "0.01", message = "Bid amount must be greater than 0")
    private BigDecimal amount;

    public UpdateHighestBidRequest() {
    }

    public UpdateHighestBidRequest(Long bidderId, BigDecimal amount) {
        this.bidderId = bidderId;
        this.amount = amount;
    }

    public static UpdateHighestBidRequestBuilder builder() {
        return new UpdateHighestBidRequestBuilder();
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

    public static class UpdateHighestBidRequestBuilder {
        private Long bidderId;
        private BigDecimal amount;

        UpdateHighestBidRequestBuilder() {
        }

        public UpdateHighestBidRequestBuilder bidderId(Long bidderId) {
            this.bidderId = bidderId;
            return this;
        }

        public UpdateHighestBidRequestBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public UpdateHighestBidRequest build() {
            return new UpdateHighestBidRequest(bidderId, amount);
        }
    }
}

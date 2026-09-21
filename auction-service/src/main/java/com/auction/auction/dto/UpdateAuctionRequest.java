package com.auction.auction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class UpdateAuctionRequest {

    @Size(min = 3, max = 128, message = "Title must be between 3 and 128 characters")
    private String title;

    private String description;

    @Size(max = 64, message = "Category must not exceed 64 characters")
    private String category;

    @DecimalMin(value = "0.01", message = "Starting price must be greater than 0")
    private BigDecimal startingPrice;

    @DecimalMin(value = "0.01", message = "Reserve price must be greater than 0")
    private BigDecimal reservePrice;

    @DecimalMin(value = "0.01", message = "Minimum increment must be greater than 0")
    private BigDecimal minimumIncrement;

    private BigDecimal minBidIncrement;

    private LocalDateTime endTime;

    public UpdateAuctionRequest() {
    }

    public UpdateAuctionRequest(String title, String description, String category, BigDecimal startingPrice,
                                BigDecimal reservePrice, BigDecimal minimumIncrement, LocalDateTime endTime) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.startingPrice = startingPrice;
        this.reservePrice = reservePrice;
        this.minimumIncrement = minimumIncrement;
        this.minBidIncrement = minimumIncrement;
        this.endTime = endTime;
    }

    public static UpdateAuctionRequestBuilder builder() {
        return new UpdateAuctionRequestBuilder();
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

    public BigDecimal getMinimumIncrement() {
        if (minimumIncrement != null) return minimumIncrement;
        return minBidIncrement;
    }

    public void setMinimumIncrement(BigDecimal minimumIncrement) {
        this.minimumIncrement = minimumIncrement;
        this.minBidIncrement = minimumIncrement;
    }

    public BigDecimal getMinBidIncrement() {
        return getMinimumIncrement();
    }

    public void setMinBidIncrement(BigDecimal minBidIncrement) {
        this.minBidIncrement = minBidIncrement;
        this.minimumIncrement = minBidIncrement;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public static class UpdateAuctionRequestBuilder {
        private String title;
        private String description;
        private String category;
        private BigDecimal startingPrice;
        private BigDecimal reservePrice;
        private BigDecimal minimumIncrement;
        private LocalDateTime endTime;

        UpdateAuctionRequestBuilder() {
        }

        public UpdateAuctionRequestBuilder title(String title) {
            this.title = title;
            return this;
        }

        public UpdateAuctionRequestBuilder description(String description) {
            this.description = description;
            return this;
        }

        public UpdateAuctionRequestBuilder category(String category) {
            this.category = category;
            return this;
        }

        public UpdateAuctionRequestBuilder startingPrice(BigDecimal startingPrice) {
            this.startingPrice = startingPrice;
            return this;
        }

        public UpdateAuctionRequestBuilder reservePrice(BigDecimal reservePrice) {
            this.reservePrice = reservePrice;
            return this;
        }

        public UpdateAuctionRequestBuilder minimumIncrement(BigDecimal minimumIncrement) {
            this.minimumIncrement = minimumIncrement;
            return this;
        }

        public UpdateAuctionRequestBuilder minBidIncrement(BigDecimal minBidIncrement) {
            this.minimumIncrement = minBidIncrement;
            return this;
        }

        public UpdateAuctionRequestBuilder endTime(LocalDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public UpdateAuctionRequest build() {
            return new UpdateAuctionRequest(title, description, category, startingPrice, reservePrice, minimumIncrement, endTime);
        }
    }
}

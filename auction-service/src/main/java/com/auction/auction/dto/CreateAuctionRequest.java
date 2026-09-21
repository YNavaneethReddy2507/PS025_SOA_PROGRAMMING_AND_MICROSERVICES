package com.auction.auction.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CreateAuctionRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 128, message = "Title must be between 3 and 128 characters")
    private String title;

    private String description;

    @Size(max = 64, message = "Category must not exceed 64 characters")
    private String category;

    @NotNull(message = "Starting price is required")
    @DecimalMin(value = "0.01", message = "Starting price must be greater than 0")
    private BigDecimal startingPrice;

    private BigDecimal reservePrice;

    @DecimalMin(value = "0.01", message = "Minimum increment must be greater than 0")
    private BigDecimal minimumIncrement;

    private BigDecimal minBidIncrement;

    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    public CreateAuctionRequest() {
    }

    public CreateAuctionRequest(String title, String description, String category, BigDecimal startingPrice,
                                BigDecimal reservePrice, BigDecimal minimumIncrement, LocalDateTime startTime, LocalDateTime endTime) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.startingPrice = startingPrice;
        this.reservePrice = reservePrice;
        this.minimumIncrement = minimumIncrement;
        this.minBidIncrement = minimumIncrement;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static CreateAuctionRequestBuilder builder() {
        return new CreateAuctionRequestBuilder();
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
        return category != null ? category : "GENERAL";
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
        return reservePrice != null ? reservePrice : startingPrice;
    }

    public void setReservePrice(BigDecimal reservePrice) {
        this.reservePrice = reservePrice;
    }

    public BigDecimal getMinimumIncrement() {
        if (minimumIncrement != null) return minimumIncrement;
        if (minBidIncrement != null) return minBidIncrement;
        return BigDecimal.ONE;
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

    public LocalDateTime getStartTime() {
        return startTime != null ? startTime : LocalDateTime.now();
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

    public static class CreateAuctionRequestBuilder {
        private String title;
        private String description;
        private String category = "GENERAL";
        private BigDecimal startingPrice;
        private BigDecimal reservePrice;
        private BigDecimal minimumIncrement;
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        CreateAuctionRequestBuilder() {
        }

        public CreateAuctionRequestBuilder title(String title) {
            this.title = title;
            return this;
        }

        public CreateAuctionRequestBuilder description(String description) {
            this.description = description;
            return this;
        }

        public CreateAuctionRequestBuilder category(String category) {
            this.category = category;
            return this;
        }

        public CreateAuctionRequestBuilder startingPrice(BigDecimal startingPrice) {
            this.startingPrice = startingPrice;
            return this;
        }

        public CreateAuctionRequestBuilder reservePrice(BigDecimal reservePrice) {
            this.reservePrice = reservePrice;
            return this;
        }

        public CreateAuctionRequestBuilder minimumIncrement(BigDecimal minimumIncrement) {
            this.minimumIncrement = minimumIncrement;
            return this;
        }

        public CreateAuctionRequestBuilder minBidIncrement(BigDecimal minBidIncrement) {
            this.minimumIncrement = minBidIncrement;
            return this;
        }

        public CreateAuctionRequestBuilder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public CreateAuctionRequestBuilder endTime(LocalDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public CreateAuctionRequest build() {
            return new CreateAuctionRequest(title, description, category, startingPrice, reservePrice, minimumIncrement, startTime, endTime);
        }
    }
}

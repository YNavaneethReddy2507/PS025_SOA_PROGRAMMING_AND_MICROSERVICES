package com.auction.auction.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CreateAuctionRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 128, message = "Title must not exceed 128 characters")
    private String title;

    private String description;

    @NotBlank(message = "Category is required")
    @Size(max = 64, message = "Category must not exceed 64 characters")
    private String category;

    @NotNull(message = "Starting price is required")
    @DecimalMin(value = "0.01", message = "Starting price must be greater than 0")
    private BigDecimal startingPrice;

    @NotNull(message = "Reserve price is required")
    @DecimalMin(value = "0.01", message = "Reserve price must be greater than 0")
    private BigDecimal reservePrice;

    @NotNull(message = "Minimum bid increment is required")
    @DecimalMin(value = "0.01", message = "Minimum bid increment must be greater than 0")
    private BigDecimal minBidIncrement;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    public CreateAuctionRequest() {
    }

    public CreateAuctionRequest(String title, String description, String category, BigDecimal startingPrice,
                                BigDecimal reservePrice, BigDecimal minBidIncrement, LocalDateTime startTime, LocalDateTime endTime) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.startingPrice = startingPrice;
        this.reservePrice = reservePrice;
        this.minBidIncrement = minBidIncrement;
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

    public static class CreateAuctionRequestBuilder {
        private String title;
        private String description;
        private String category;
        private BigDecimal startingPrice;
        private BigDecimal reservePrice;
        private BigDecimal minBidIncrement;
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

        public CreateAuctionRequestBuilder minBidIncrement(BigDecimal minBidIncrement) {
            this.minBidIncrement = minBidIncrement;
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
            return new CreateAuctionRequest(title, description, category, startingPrice, reservePrice, minBidIncrement, startTime, endTime);
        }
    }
}

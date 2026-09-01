package com.auction.auction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class UpdateAuctionRequest {

    @Size(max = 128, message = "Title must not exceed 128 characters")
    private String title;

    private String description;

    @Size(max = 64, message = "Category must not exceed 64 characters")
    private String category;

    @DecimalMin(value = "0.01", message = "Reserve price must be greater than 0")
    private BigDecimal reservePrice;

    @DecimalMin(value = "0.01", message = "Minimum bid increment must be greater than 0")
    private BigDecimal minBidIncrement;

    @Future(message = "End time must be in the future")
    private LocalDateTime endTime;

    public UpdateAuctionRequest() {
    }

    public UpdateAuctionRequest(String title, String description, String category, BigDecimal reservePrice, BigDecimal minBidIncrement, LocalDateTime endTime) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.reservePrice = reservePrice;
        this.minBidIncrement = minBidIncrement;
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
        private BigDecimal reservePrice;
        private BigDecimal minBidIncrement;
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

        public UpdateAuctionRequestBuilder reservePrice(BigDecimal reservePrice) {
            this.reservePrice = reservePrice;
            return this;
        }

        public UpdateAuctionRequestBuilder minBidIncrement(BigDecimal minBidIncrement) {
            this.minBidIncrement = minBidIncrement;
            return this;
        }

        public UpdateAuctionRequestBuilder endTime(LocalDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public UpdateAuctionRequest build() {
            return new UpdateAuctionRequest(title, description, category, reservePrice, minBidIncrement, endTime);
        }
    }
}

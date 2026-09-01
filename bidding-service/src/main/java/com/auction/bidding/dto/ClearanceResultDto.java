package com.auction.bidding.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ClearanceResultDto {

    private Long auctionId;
    private Long winningBidderId;
    private BigDecimal winningAmount;
    private boolean reserveMet;
    private LocalDateTime clearedAt;
    private String clearanceStatus;
    private String message;

    public ClearanceResultDto() {
    }

    public ClearanceResultDto(Long auctionId, Long winningBidderId, BigDecimal winningAmount, boolean reserveMet,
                              LocalDateTime clearedAt, String clearanceStatus, String message) {
        this.auctionId = auctionId;
        this.winningBidderId = winningBidderId;
        this.winningAmount = winningAmount;
        this.reserveMet = reserveMet;
        this.clearedAt = clearedAt;
        this.clearanceStatus = clearanceStatus;
        this.message = message;
    }

    public static ClearanceResultDtoBuilder builder() {
        return new ClearanceResultDtoBuilder();
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
    }

    public Long getWinningBidderId() {
        return winningBidderId;
    }

    public void setWinningBidderId(Long winningBidderId) {
        this.winningBidderId = winningBidderId;
    }

    public BigDecimal getWinningAmount() {
        return winningAmount;
    }

    public void setWinningAmount(BigDecimal winningAmount) {
        this.winningAmount = winningAmount;
    }

    public boolean isReserveMet() {
        return reserveMet;
    }

    public void setReserveMet(boolean reserveMet) {
        this.reserveMet = reserveMet;
    }

    public LocalDateTime getClearedAt() {
        return clearedAt;
    }

    public void setClearedAt(LocalDateTime clearedAt) {
        this.clearedAt = clearedAt;
    }

    public String getClearanceStatus() {
        return clearanceStatus;
    }

    public void setClearanceStatus(String clearanceStatus) {
        this.clearanceStatus = clearanceStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static class ClearanceResultDtoBuilder {
        private Long auctionId;
        private Long winningBidderId;
        private BigDecimal winningAmount;
        private boolean reserveMet;
        private LocalDateTime clearedAt;
        private String clearanceStatus;
        private String message;

        ClearanceResultDtoBuilder() {
        }

        public ClearanceResultDtoBuilder auctionId(Long auctionId) {
            this.auctionId = auctionId;
            return this;
        }

        public ClearanceResultDtoBuilder winningBidderId(Long winningBidderId) {
            this.winningBidderId = winningBidderId;
            return this;
        }

        public ClearanceResultDtoBuilder winningAmount(BigDecimal winningAmount) {
            this.winningAmount = winningAmount;
            return this;
        }

        public ClearanceResultDtoBuilder reserveMet(boolean reserveMet) {
            this.reserveMet = reserveMet;
            return this;
        }

        public ClearanceResultDtoBuilder clearedAt(LocalDateTime clearedAt) {
            this.clearedAt = clearedAt;
            return this;
        }

        public ClearanceResultDtoBuilder clearanceStatus(String clearanceStatus) {
            this.clearanceStatus = clearanceStatus;
            return this;
        }

        public ClearanceResultDtoBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ClearanceResultDto build() {
            return new ClearanceResultDto(auctionId, winningBidderId, winningAmount, reserveMet, clearedAt, clearanceStatus, message);
        }
    }
}

package com.auction.auction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentSettlementResponse {

    private String transactionId;
    private Long auctionId;
    private Long sellerId;
    private Long buyerId;
    private BigDecimal amount;
    private BigDecimal platformFee;
    private BigDecimal netSellerPayout;
    private String status;
    private LocalDateTime settledAt;

    public PaymentSettlementResponse() {
    }

    public PaymentSettlementResponse(String transactionId, Long auctionId, Long sellerId, Long buyerId,
                                   BigDecimal amount, BigDecimal platformFee, BigDecimal netSellerPayout,
                                   String status, LocalDateTime settledAt) {
        this.transactionId = transactionId;
        this.auctionId = auctionId;
        this.sellerId = sellerId;
        this.buyerId = buyerId;
        this.amount = amount;
        this.platformFee = platformFee;
        this.netSellerPayout = netSellerPayout;
        this.status = status;
        this.settledAt = settledAt;
    }

    public static PaymentSettlementResponseBuilder builder() {
        return new PaymentSettlementResponseBuilder();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getPlatformFee() {
        return platformFee;
    }

    public void setPlatformFee(BigDecimal platformFee) {
        this.platformFee = platformFee;
    }

    public BigDecimal getNetSellerPayout() {
        return netSellerPayout;
    }

    public void setNetSellerPayout(BigDecimal netSellerPayout) {
        this.netSellerPayout = netSellerPayout;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getSettledAt() {
        return settledAt;
    }

    public void setSettledAt(LocalDateTime settledAt) {
        this.settledAt = settledAt;
    }

    public static class PaymentSettlementResponseBuilder {
        private String transactionId;
        private Long auctionId;
        private Long sellerId;
        private Long buyerId;
        private BigDecimal amount;
        private BigDecimal platformFee;
        private BigDecimal netSellerPayout;
        private String status;
        private LocalDateTime settledAt;

        PaymentSettlementResponseBuilder() {
        }

        public PaymentSettlementResponseBuilder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public PaymentSettlementResponseBuilder auctionId(Long auctionId) {
            this.auctionId = auctionId;
            return this;
        }

        public PaymentSettlementResponseBuilder sellerId(Long sellerId) {
            this.sellerId = sellerId;
            return this;
        }

        public PaymentSettlementResponseBuilder buyerId(Long buyerId) {
            this.buyerId = buyerId;
            return this;
        }

        public PaymentSettlementResponseBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public PaymentSettlementResponseBuilder platformFee(BigDecimal platformFee) {
            this.platformFee = platformFee;
            return this;
        }

        public PaymentSettlementResponseBuilder netSellerPayout(BigDecimal netSellerPayout) {
            this.netSellerPayout = netSellerPayout;
            return this;
        }

        public PaymentSettlementResponseBuilder status(String status) {
            this.status = status;
            return this;
        }

        public PaymentSettlementResponseBuilder settledAt(LocalDateTime settledAt) {
            this.settledAt = settledAt;
            return this;
        }

        public PaymentSettlementResponse build() {
            return new PaymentSettlementResponse(transactionId, auctionId, sellerId, buyerId, amount, platformFee, netSellerPayout, status, settledAt);
        }
    }
}

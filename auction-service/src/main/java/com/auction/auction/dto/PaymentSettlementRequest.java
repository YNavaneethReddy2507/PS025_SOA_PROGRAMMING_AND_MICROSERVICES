package com.auction.auction.dto;

import java.math.BigDecimal;

public class PaymentSettlementRequest {

    private Long auctionId;
    private Long sellerId;
    private Long buyerId;
    private BigDecimal amount;

    public PaymentSettlementRequest() {
    }

    public PaymentSettlementRequest(Long auctionId, Long sellerId, Long buyerId, BigDecimal amount) {
        this.auctionId = auctionId;
        this.sellerId = sellerId;
        this.buyerId = buyerId;
        this.amount = amount;
    }

    public static PaymentSettlementRequestBuilder builder() {
        return new PaymentSettlementRequestBuilder();
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

    public static class PaymentSettlementRequestBuilder {
        private Long auctionId;
        private Long sellerId;
        private Long buyerId;
        private BigDecimal amount;

        PaymentSettlementRequestBuilder() {
        }

        public PaymentSettlementRequestBuilder auctionId(Long auctionId) {
            this.auctionId = auctionId;
            return this;
        }

        public PaymentSettlementRequestBuilder sellerId(Long sellerId) {
            this.sellerId = sellerId;
            return this;
        }

        public PaymentSettlementRequestBuilder buyerId(Long buyerId) {
            this.buyerId = buyerId;
            return this;
        }

        public PaymentSettlementRequestBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public PaymentSettlementRequest build() {
            return new PaymentSettlementRequest(auctionId, sellerId, buyerId, amount);
        }
    }
}

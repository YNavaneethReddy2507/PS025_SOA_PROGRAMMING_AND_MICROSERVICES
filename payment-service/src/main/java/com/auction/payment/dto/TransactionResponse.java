package com.auction.payment.dto;

import com.auction.payment.entity.PaymentStatus;
import com.auction.payment.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionResponse {

    private Long id;
    private String transactionId;
    private Long auctionId;
    private Long buyerId;
    private Long sellerId;
    private BigDecimal amount;
    private BigDecimal platformFee;
    private BigDecimal netSellerPayout;
    private PaymentStatus status;
    private TransactionType type;
    private String remarks;
    private LocalDateTime createdAt;

    public TransactionResponse() {
    }

    public TransactionResponse(Long id, String transactionId, Long auctionId, Long buyerId, Long sellerId,
                               BigDecimal amount, BigDecimal platformFee, BigDecimal netSellerPayout,
                               PaymentStatus status, TransactionType type, String remarks, LocalDateTime createdAt) {
        this.id = id;
        this.transactionId = transactionId;
        this.auctionId = auctionId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.amount = amount;
        this.platformFee = platformFee;
        this.netSellerPayout = netSellerPayout;
        this.status = status;
        this.type = type;
        this.remarks = remarks;
        this.createdAt = createdAt;
    }

    public static TransactionResponseBuilder builder() {
        return new TransactionResponseBuilder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
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

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static class TransactionResponseBuilder {
        private Long id;
        private String transactionId;
        private Long auctionId;
        private Long buyerId;
        private Long sellerId;
        private BigDecimal amount;
        private BigDecimal platformFee;
        private BigDecimal netSellerPayout;
        private PaymentStatus status;
        private TransactionType type;
        private String remarks;
        private LocalDateTime createdAt;

        TransactionResponseBuilder() {
        }

        public TransactionResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public TransactionResponseBuilder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public TransactionResponseBuilder auctionId(Long auctionId) {
            this.auctionId = auctionId;
            return this;
        }

        public TransactionResponseBuilder buyerId(Long buyerId) {
            this.buyerId = buyerId;
            return this;
        }

        public TransactionResponseBuilder sellerId(Long sellerId) {
            this.sellerId = sellerId;
            return this;
        }

        public TransactionResponseBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public TransactionResponseBuilder platformFee(BigDecimal platformFee) {
            this.platformFee = platformFee;
            return this;
        }

        public TransactionResponseBuilder netSellerPayout(BigDecimal netSellerPayout) {
            this.netSellerPayout = netSellerPayout;
            return this;
        }

        public TransactionResponseBuilder status(PaymentStatus status) {
            this.status = status;
            return this;
        }

        public TransactionResponseBuilder type(TransactionType type) {
            this.type = type;
            return this;
        }

        public TransactionResponseBuilder remarks(String remarks) {
            this.remarks = remarks;
            return this;
        }

        public TransactionResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public TransactionResponse build() {
            return new TransactionResponse(id, transactionId, auctionId, buyerId, sellerId, amount, platformFee, netSellerPayout, status, type, remarks, createdAt);
        }
    }
}

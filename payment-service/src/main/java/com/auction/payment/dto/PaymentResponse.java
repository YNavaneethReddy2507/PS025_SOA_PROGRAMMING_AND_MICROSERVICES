package com.auction.payment.dto;

import com.auction.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentResponse {

    private Long id;
    private Long auctionId;
    private Long winnerId;
    private BigDecimal amount;
    private PaymentStatus status;
    private String transactionReference;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private String message;

    public PaymentResponse() {
    }

    public PaymentResponse(Long id, Long auctionId, Long winnerId, BigDecimal amount,
                           PaymentStatus status, String transactionReference,
                           LocalDateTime createdAt, LocalDateTime paidAt, String message) {
        this.id = id;
        this.auctionId = auctionId;
        this.winnerId = winnerId;
        this.amount = amount;
        this.status = status;
        this.transactionReference = transactionReference;
        this.createdAt = createdAt;
        this.paidAt = paidAt;
        this.message = message;
    }

    public static PaymentResponseBuilder builder() {
        return new PaymentResponseBuilder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
    }

    public Long getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(Long winnerId) {
        this.winnerId = winnerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static class PaymentResponseBuilder {
        private Long id;
        private Long auctionId;
        private Long winnerId;
        private BigDecimal amount;
        private PaymentStatus status;
        private String transactionReference;
        private LocalDateTime createdAt;
        private LocalDateTime paidAt;
        private String message;

        public PaymentResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PaymentResponseBuilder auctionId(Long auctionId) {
            this.auctionId = auctionId;
            return this;
        }

        public PaymentResponseBuilder winnerId(Long winnerId) {
            this.winnerId = winnerId;
            return this;
        }

        public PaymentResponseBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public PaymentResponseBuilder status(PaymentStatus status) {
            this.status = status;
            return this;
        }

        public PaymentResponseBuilder transactionReference(String transactionReference) {
            this.transactionReference = transactionReference;
            return this;
        }

        public PaymentResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public PaymentResponseBuilder paidAt(LocalDateTime paidAt) {
            this.paidAt = paidAt;
            return this;
        }

        public PaymentResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public PaymentResponse build() {
            return new PaymentResponse(id, auctionId, winnerId, amount, status, transactionReference, createdAt, paidAt, message);
        }
    }
}

package com.auction.payment.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payment_auction", columnList = "auction_id"),
                @Index(name = "idx_payment_winner", columnList = "winner_id"),
                @Index(name = "idx_payment_tx_ref", columnList = "transaction_reference", unique = true)
        }
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auction_id", nullable = false)
    private Long auctionId;

    @Column(name = "winner_id", nullable = false)
    private Long winnerId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "transaction_reference", nullable = false, unique = true, length = 64)
    private String transactionReference;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Version
    private Long version;

    public Payment() {
    }

    public Payment(Long id, Long auctionId, Long winnerId, BigDecimal amount,
                   PaymentStatus status, String transactionReference,
                   LocalDateTime createdAt, LocalDateTime paidAt, Long version) {
        this.id = id;
        this.auctionId = auctionId;
        this.winnerId = winnerId;
        this.amount = amount;
        this.status = status;
        this.transactionReference = transactionReference;
        this.createdAt = createdAt;
        this.paidAt = paidAt;
        this.version = version;
    }

    public static PaymentBuilder builder() {
        return new PaymentBuilder();
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public static class PaymentBuilder {
        private Long id;
        private Long auctionId;
        private Long winnerId;
        private BigDecimal amount;
        private PaymentStatus status;
        private String transactionReference;
        private LocalDateTime createdAt;
        private LocalDateTime paidAt;
        private Long version;

        public PaymentBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PaymentBuilder auctionId(Long auctionId) {
            this.auctionId = auctionId;
            return this;
        }

        public PaymentBuilder winnerId(Long winnerId) {
            this.winnerId = winnerId;
            return this;
        }

        public PaymentBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public PaymentBuilder status(PaymentStatus status) {
            this.status = status;
            return this;
        }

        public PaymentBuilder transactionReference(String transactionReference) {
            this.transactionReference = transactionReference;
            return this;
        }

        public PaymentBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public PaymentBuilder paidAt(LocalDateTime paidAt) {
            this.paidAt = paidAt;
            return this;
        }

        public PaymentBuilder version(Long version) {
            this.version = version;
            return this;
        }

        public Payment build() {
            return new Payment(id, auctionId, winnerId, amount, status, transactionReference, createdAt, paidAt, version);
        }
    }
}

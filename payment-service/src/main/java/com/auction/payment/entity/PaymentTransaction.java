package com.auction.payment.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payment_transactions",
        indexes = {
                @Index(name = "idx_tx_id", columnList = "transaction_id", unique = true),
                @Index(name = "idx_tx_auction_id", columnList = "auction_id")
        }
)
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 64)
    private String transactionId;

    @Column(name = "auction_id")
    private Long auctionId;

    @Column(name = "buyer_id")
    private Long buyerId;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "platform_fee", nullable = false, precision = 19, scale = 4)
    private BigDecimal platformFee;

    @Column(name = "net_seller_payout", nullable = false, precision = 19, scale = 4)
    private BigDecimal netSellerPayout;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(length = 255)
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public PaymentTransaction() {
    }

    public PaymentTransaction(Long id, String transactionId, Long auctionId, Long buyerId, Long sellerId,
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

    public static PaymentTransactionBuilder builder() {
        return new PaymentTransactionBuilder();
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

    public static class PaymentTransactionBuilder {
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

        PaymentTransactionBuilder() {
        }

        public PaymentTransactionBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PaymentTransactionBuilder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public PaymentTransactionBuilder auctionId(Long auctionId) {
            this.auctionId = auctionId;
            return this;
        }

        public PaymentTransactionBuilder buyerId(Long buyerId) {
            this.buyerId = buyerId;
            return this;
        }

        public PaymentTransactionBuilder sellerId(Long sellerId) {
            this.sellerId = sellerId;
            return this;
        }

        public PaymentTransactionBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public PaymentTransactionBuilder platformFee(BigDecimal platformFee) {
            this.platformFee = platformFee;
            return this;
        }

        public PaymentTransactionBuilder netSellerPayout(BigDecimal netSellerPayout) {
            this.netSellerPayout = netSellerPayout;
            return this;
        }

        public PaymentTransactionBuilder status(PaymentStatus status) {
            this.status = status;
            return this;
        }

        public PaymentTransactionBuilder type(TransactionType type) {
            this.type = type;
            return this;
        }

        public PaymentTransactionBuilder remarks(String remarks) {
            this.remarks = remarks;
            return this;
        }

        public PaymentTransactionBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public PaymentTransaction build() {
            return new PaymentTransaction(id, transactionId, auctionId, buyerId, sellerId, amount, platformFee, netSellerPayout, status, type, remarks, createdAt);
        }
    }
}

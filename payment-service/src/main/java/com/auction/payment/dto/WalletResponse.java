package com.auction.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletResponse {

    private Long id;
    private Long userId;
    private BigDecimal balance;
    private BigDecimal lockedBalance;
    private BigDecimal totalBalance;
    private LocalDateTime updatedAt;

    public WalletResponse() {
    }

    public WalletResponse(Long id, Long userId, BigDecimal balance, BigDecimal lockedBalance, BigDecimal totalBalance, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.balance = balance;
        this.lockedBalance = lockedBalance;
        this.totalBalance = totalBalance;
        this.updatedAt = updatedAt;
    }

    public static WalletResponseBuilder builder() {
        return new WalletResponseBuilder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getLockedBalance() {
        return lockedBalance;
    }

    public void setLockedBalance(BigDecimal lockedBalance) {
        this.lockedBalance = lockedBalance;
    }

    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(BigDecimal totalBalance) {
        this.totalBalance = totalBalance;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static class WalletResponseBuilder {
        private Long id;
        private Long userId;
        private BigDecimal balance;
        private BigDecimal lockedBalance;
        private BigDecimal totalBalance;
        private LocalDateTime updatedAt;

        WalletResponseBuilder() {
        }

        public WalletResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public WalletResponseBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public WalletResponseBuilder balance(BigDecimal balance) {
            this.balance = balance;
            return this;
        }

        public WalletResponseBuilder lockedBalance(BigDecimal lockedBalance) {
            this.lockedBalance = lockedBalance;
            return this;
        }

        public WalletResponseBuilder totalBalance(BigDecimal totalBalance) {
            this.totalBalance = totalBalance;
            return this;
        }

        public WalletResponseBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public WalletResponse build() {
            return new WalletResponse(id, userId, balance, lockedBalance, totalBalance, updatedAt);
        }
    }
}

package com.auction.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class WalletDepositRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Deposit amount must be greater than 0")
    private BigDecimal amount;

    public WalletDepositRequest() {
    }

    public WalletDepositRequest(BigDecimal amount) {
        this.amount = amount;
    }

    public static WalletDepositRequestBuilder builder() {
        return new WalletDepositRequestBuilder();
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public static class WalletDepositRequestBuilder {
        private BigDecimal amount;

        WalletDepositRequestBuilder() {
        }

        public WalletDepositRequestBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public WalletDepositRequest build() {
            return new WalletDepositRequest(amount);
        }
    }
}

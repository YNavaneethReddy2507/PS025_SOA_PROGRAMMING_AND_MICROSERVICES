package com.auction.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class PaymentRequest {

    @NotNull(message = "Auction ID is required")
    private Long auctionId;

    private Long winnerId;

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
    private BigDecimal amount;

    private String paymentMethod;
    private String maskedCardNumber;
    private boolean simulateFailure;

    public PaymentRequest() {
    }

    public PaymentRequest(Long auctionId, Long winnerId, BigDecimal amount,
                          String paymentMethod, String maskedCardNumber, boolean simulateFailure) {
        this.auctionId = auctionId;
        this.winnerId = winnerId;
        this.amount = amount;
        this.paymentMethod = paymentMethod != null ? paymentMethod : "CREDIT_CARD";
        this.maskedCardNumber = maskedCardNumber != null ? maskedCardNumber : "**** 4242";
        this.simulateFailure = simulateFailure;
    }

    public static PaymentRequestBuilder builder() {
        return new PaymentRequestBuilder();
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

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getMaskedCardNumber() {
        return maskedCardNumber;
    }

    public void setMaskedCardNumber(String maskedCardNumber) {
        this.maskedCardNumber = maskedCardNumber;
    }

    public boolean isSimulateFailure() {
        return simulateFailure;
    }

    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }

    public static class PaymentRequestBuilder {
        private Long auctionId;
        private Long winnerId;
        private BigDecimal amount;
        private String paymentMethod = "CREDIT_CARD";
        private String maskedCardNumber = "**** 4242";
        private boolean simulateFailure = false;

        public PaymentRequestBuilder auctionId(Long auctionId) {
            this.auctionId = auctionId;
            return this;
        }

        public PaymentRequestBuilder winnerId(Long winnerId) {
            this.winnerId = winnerId;
            return this;
        }

        public PaymentRequestBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public PaymentRequestBuilder paymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public PaymentRequestBuilder maskedCardNumber(String maskedCardNumber) {
            this.maskedCardNumber = maskedCardNumber;
            return this;
        }

        public PaymentRequestBuilder simulateFailure(boolean simulateFailure) {
            this.simulateFailure = simulateFailure;
            return this;
        }

        public PaymentRequest build() {
            return new PaymentRequest(auctionId, winnerId, amount, paymentMethod, maskedCardNumber, simulateFailure);
        }
    }
}

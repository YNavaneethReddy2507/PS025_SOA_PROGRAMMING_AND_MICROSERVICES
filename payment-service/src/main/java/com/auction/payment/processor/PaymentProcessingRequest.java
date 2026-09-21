package com.auction.payment.processor;

import java.math.BigDecimal;

public class PaymentProcessingRequest {

    private Long auctionId;
    private Long winnerId;
    private BigDecimal amount;
    private String paymentMethod;
    private String maskedCardNumber;
    private boolean simulateFailure;

    public PaymentProcessingRequest() {
    }

    public PaymentProcessingRequest(Long auctionId, Long winnerId, BigDecimal amount,
                                  String paymentMethod, String maskedCardNumber, boolean simulateFailure) {
        this.auctionId = auctionId;
        this.winnerId = winnerId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.maskedCardNumber = maskedCardNumber;
        this.simulateFailure = simulateFailure;
    }

    public static PaymentProcessingRequestBuilder builder() {
        return new PaymentProcessingRequestBuilder();
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

    public static class PaymentProcessingRequestBuilder {
        private Long auctionId;
        private Long winnerId;
        private BigDecimal amount;
        private String paymentMethod = "CREDIT_CARD";
        private String maskedCardNumber = "**** 4242";
        private boolean simulateFailure = false;

        public PaymentProcessingRequestBuilder auctionId(Long auctionId) {
            this.auctionId = auctionId;
            return this;
        }

        public PaymentProcessingRequestBuilder winnerId(Long winnerId) {
            this.winnerId = winnerId;
            return this;
        }

        public PaymentProcessingRequestBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public PaymentProcessingRequestBuilder paymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public PaymentProcessingRequestBuilder maskedCardNumber(String maskedCardNumber) {
            this.maskedCardNumber = maskedCardNumber;
            return this;
        }

        public PaymentProcessingRequestBuilder simulateFailure(boolean simulateFailure) {
            this.simulateFailure = simulateFailure;
            return this;
        }

        public PaymentProcessingRequest build() {
            return new PaymentProcessingRequest(auctionId, winnerId, amount, paymentMethod, maskedCardNumber, simulateFailure);
        }
    }
}

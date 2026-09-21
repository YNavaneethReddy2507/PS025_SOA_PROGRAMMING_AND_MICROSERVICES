package com.auction.payment.processor;

import java.time.LocalDateTime;

public class PaymentProcessingResult {

    private boolean success;
    private String transactionReference;
    private String message;
    private String errorCode;
    private LocalDateTime processedAt;

    public PaymentProcessingResult() {
    }

    public PaymentProcessingResult(boolean success, String transactionReference, String message, String errorCode, LocalDateTime processedAt) {
        this.success = success;
        this.transactionReference = transactionReference;
        this.message = message;
        this.errorCode = errorCode;
        this.processedAt = processedAt;
    }

    public static PaymentProcessingResult success(String transactionReference, String message) {
        return new PaymentProcessingResult(true, transactionReference, message, null, LocalDateTime.now());
    }

    public static PaymentProcessingResult failure(String transactionReference, String message, String errorCode) {
        return new PaymentProcessingResult(false, transactionReference, message, errorCode, LocalDateTime.now());
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}

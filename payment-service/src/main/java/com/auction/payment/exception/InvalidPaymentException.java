package com.auction.payment.exception;

public class InvalidPaymentException extends RuntimeException {

    public InvalidPaymentException(String message) {
        super(message);
    }
}

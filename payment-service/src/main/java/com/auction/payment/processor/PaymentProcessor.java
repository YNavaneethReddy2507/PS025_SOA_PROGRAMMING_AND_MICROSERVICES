package com.auction.payment.processor;

/**
 * Pluggable Payment Processor interface.
 * Abstracts third-party payment gateway integration (Stripe, PayPal, Mock, etc.).
 */
public interface PaymentProcessor {

    PaymentProcessingResult process(PaymentProcessingRequest request);
}

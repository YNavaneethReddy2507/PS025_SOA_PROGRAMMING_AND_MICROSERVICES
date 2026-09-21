package com.auction.payment.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MockPaymentProcessor implements PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentProcessor.class);

    // Simulated failure amount sentinel: $99999.00 triggers simulated card decline
    public static final BigDecimal DECLINE_AMOUNT = new BigDecimal("99999.00");

    @Override
    public PaymentProcessingResult process(PaymentProcessingRequest request) {
        String txRef = "PAY-" + UUID.randomUUID().toString().substring(0, 18).toUpperCase();

        log.info("MockPaymentProcessor initiating transaction: ref={}, auctionId={}, winnerId={}, amount={}, method={}",
                txRef, request.getAuctionId(), request.getWinnerId(), request.getAmount(), request.getPaymentMethod());

        // Sensitive credentials (CVV, full card number) are NEVER logged or stored

        // 1. Check for explicit simulated failure flag
        if (request.isSimulateFailure()) {
            log.warn("MockPaymentProcessor: Simulated payment failure for txRef: {}", txRef);
            return PaymentProcessingResult.failure(txRef, "Card was declined by issuing bank", "CARD_DECLINED");
        }

        // 2. Check for sentinel decline amount
        if (request.getAmount() != null && request.getAmount().compareTo(DECLINE_AMOUNT) == 0) {
            log.warn("MockPaymentProcessor: Simulated card decline for sentinel amount: {}", DECLINE_AMOUNT);
            return PaymentProcessingResult.failure(txRef, "Transaction exceeded daily limit", "LIMIT_EXCEEDED");
        }

        // 3. Successful processing simulation
        log.info("MockPaymentProcessor: Transaction approved successfully: ref={}", txRef);
        return PaymentProcessingResult.success(txRef, "Payment authorized and settled successfully");
    }
}

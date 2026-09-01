package com.auction.payment.controller;

import com.auction.payment.dto.*;
import com.auction.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/wallet/deposit")
    public ResponseEntity<WalletResponse> deposit(
            @Valid @RequestBody WalletDepositRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        Long userId = headerUserId != null ? headerUserId : 1L;
        log.info("Processing deposit request for user ID: {}", userId);
        WalletResponse response = paymentService.deposit(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/wallet")
    public ResponseEntity<WalletResponse> getWallet(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId) {
        Long userId = headerUserId != null ? headerUserId : 1L;
        WalletResponse response = paymentService.getWalletByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/wallet/{userId}")
    public ResponseEntity<WalletResponse> getWalletByUserId(@PathVariable("userId") Long userId) {
        WalletResponse response = paymentService.getWalletByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/settle")
    public ResponseEntity<PaymentSettlementResponse> settlePayment(@Valid @RequestBody PaymentSettlementRequest request) {
        log.info("Received payment settlement request for auction ID: {}", request.getAuctionId());
        PaymentSettlementResponse response = paymentService.settleAuctionPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable("transactionId") String transactionId) {
        TransactionResponse response = paymentService.getTransactionById(transactionId);
        return ResponseEntity.ok(response);
    }
}

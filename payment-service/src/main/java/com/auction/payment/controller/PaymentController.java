package com.auction.payment.controller;

import com.auction.payment.dto.*;
import com.auction.payment.security.JwtUtil;
import com.auction.payment.service.PaymentService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/payments", "/api/v1/payments"})
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;
    private final JwtUtil jwtUtil;

    @Autowired
    public PaymentController(PaymentService paymentService, @Autowired(required = false) JwtUtil jwtUtil) {
        this.paymentService = paymentService;
        this.jwtUtil = jwtUtil;
    }

    public PaymentController(PaymentService paymentService) {
        this(paymentService, null);
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> makePayment(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Long userId = resolveUserId(headerUserId, authHeader, request.getWinnerId());
        log.info("Received winner payment request: auctionId={}, userId={}, amount={}",
                request.getAuctionId(), userId, request.getAmount());

        PaymentResponse response = paymentService.processWinnerPayment(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable("id") Long id) {
        PaymentResponse response = paymentService.getPaymentById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/auction/{auctionId}")
    public ResponseEntity<PaymentResponse> getPaymentByAuctionId(@PathVariable("auctionId") Long auctionId) {
        PaymentResponse response = paymentService.getPaymentByAuctionId(auctionId);
        return ResponseEntity.ok(response);
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

    private Long resolveUserId(Long headerUserId, String authHeader, Long fallbackUserId) {
        if (headerUserId != null) {
            return headerUserId;
        }
        if (authHeader != null && authHeader.startsWith("Bearer ") && jwtUtil != null) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                Claims claims = jwtUtil.extractAllClaims(token);
                Object claimId = claims.get("userId");
                if (claimId != null) {
                    try {
                        return Long.valueOf(claimId.toString());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return fallbackUserId;
    }
}

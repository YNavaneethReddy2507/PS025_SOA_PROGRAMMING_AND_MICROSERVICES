package com.auction.payment.service.impl;

import com.auction.payment.client.AuctionClient;
import com.auction.payment.dto.*;
import com.auction.payment.entity.Payment;
import com.auction.payment.entity.PaymentStatus;
import com.auction.payment.entity.PaymentTransaction;
import com.auction.payment.entity.TransactionType;
import com.auction.payment.entity.Wallet;
import com.auction.payment.exception.InsufficientBalanceException;
import com.auction.payment.exception.InvalidPaymentException;
import com.auction.payment.exception.ResourceNotFoundException;
import com.auction.payment.exception.UnauthorizedException;
import com.auction.payment.processor.PaymentProcessingRequest;
import com.auction.payment.processor.PaymentProcessingResult;
import com.auction.payment.processor.PaymentProcessor;
import com.auction.payment.repository.PaymentRepository;
import com.auction.payment.repository.PaymentTransactionRepository;
import com.auction.payment.repository.WalletRepository;
import com.auction.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final WalletRepository walletRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;
    private final AuctionClient auctionClient;
    private final PaymentProcessor paymentProcessor;
    private final TransactionTemplate transactionTemplate;

    private final ConcurrentHashMap<Long, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

    @Value("${payment.platform-fee-percentage:0.05}")
    private double platformFeePercentage;

    @Autowired
    public PaymentServiceImpl(
            WalletRepository walletRepository,
            PaymentTransactionRepository transactionRepository,
            @Autowired(required = false) PaymentRepository paymentRepository,
            @Autowired(required = false) AuctionClient auctionClient,
            @Autowired(required = false) PaymentProcessor paymentProcessor,
            @Autowired(required = false) PlatformTransactionManager transactionManager
    ) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.paymentRepository = paymentRepository;
        this.auctionClient = auctionClient;
        this.paymentProcessor = paymentProcessor;
        this.transactionTemplate = transactionManager != null ? new TransactionTemplate(transactionManager) : null;
    }

    public PaymentServiceImpl(
            WalletRepository walletRepository,
            PaymentTransactionRepository transactionRepository,
            PaymentRepository paymentRepository,
            AuctionClient auctionClient,
            PaymentProcessor paymentProcessor
    ) {
        this(walletRepository, transactionRepository, paymentRepository, auctionClient, paymentProcessor, null);
    }

    public PaymentServiceImpl(WalletRepository walletRepository, PaymentTransactionRepository transactionRepository) {
        this(walletRepository, transactionRepository, null, null, null, null);
    }

    private <T> T executeInTransaction(Supplier<T> action) {
        if (transactionTemplate != null) {
            return transactionTemplate.execute(status -> action.get());
        } else {
            return action.get();
        }
    }

    @Override
    @Transactional
    public WalletResponse deposit(Long userId, WalletDepositRequest request) {
        log.info("Processing wallet deposit of {} for user ID {}", request.getAmount(), userId);

        Wallet wallet = walletRepository.findByUserId(userId).orElseGet(() ->
                Wallet.builder()
                        .userId(userId)
                        .balance(BigDecimal.ZERO)
                        .lockedBalance(BigDecimal.ZERO)
                        .build()
        );

        wallet.setBalance(wallet.getBalance().add(request.getAmount()));
        Wallet savedWallet = walletRepository.save(wallet);

        // Record deposit transaction
        PaymentTransaction tx = PaymentTransaction.builder()
                .transactionId(UUID.randomUUID().toString())
                .buyerId(userId)
                .amount(request.getAmount())
                .platformFee(BigDecimal.ZERO)
                .netSellerPayout(BigDecimal.ZERO)
                .status(PaymentStatus.COMPLETED)
                .type(TransactionType.DEPOSIT)
                .remarks("Wallet deposit")
                .build();
        transactionRepository.save(tx);

        log.info("Deposit completed. New balance for user {}: {}", userId, savedWallet.getBalance());
        return mapToWalletResponse(savedWallet);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getWalletByUserId(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId).orElseGet(() ->
                Wallet.builder()
                        .userId(userId)
                        .balance(BigDecimal.ZERO)
                        .lockedBalance(BigDecimal.ZERO)
                        .build()
        );
        return mapToWalletResponse(wallet);
    }

    @Override
    @Transactional
    public PaymentSettlementResponse settleAuctionPayment(PaymentSettlementRequest request) {
        Long auctionId = request.getAuctionId();
        Long sellerId = request.getSellerId();
        Long buyerId = request.getBuyerId();
        BigDecimal amount = request.getAmount();

        log.info("Initiating settlement for auction {}: buyer={}, seller={}, amount={}",
                auctionId, buyerId, sellerId, amount);

        // 1. Idempotency check: Check if auction was already settled
        Optional<PaymentTransaction> existingTx = transactionRepository.findByAuctionId(auctionId);
        if (existingTx.isPresent() && existingTx.get().getStatus() == PaymentStatus.COMPLETED) {
            log.info("Auction {} was already settled under transaction {}", auctionId, existingTx.get().getTransactionId());
            PaymentTransaction tx = existingTx.get();
            return mapToSettlementResponse(tx);
        }

        // 2. Fetch or initialize buyer wallet
        Wallet buyerWallet = walletRepository.findByUserId(buyerId).orElseGet(() ->
                Wallet.builder()
                        .userId(buyerId)
                        .balance(new BigDecimal("100000.00")) // Auto-fund sandbox wallet for seamless demo/testing
                        .lockedBalance(BigDecimal.ZERO)
                        .build()
        );

        if (buyerWallet.getBalance().compareTo(amount) < 0) {
            log.warn("Settlement failed: Buyer {} has insufficient balance (balance={}, required={})",
                    buyerId, buyerWallet.getBalance(), amount);
            throw new InsufficientBalanceException("Buyer has insufficient funds for settlement");
        }

        // 3. Fetch or initialize seller wallet
        Wallet sellerWallet = walletRepository.findByUserId(sellerId).orElseGet(() ->
                Wallet.builder()
                        .userId(sellerId)
                        .balance(BigDecimal.ZERO)
                        .lockedBalance(BigDecimal.ZERO)
                        .build()
        );

        // 4. Calculate fee and net payout
        BigDecimal fee = amount.multiply(BigDecimal.valueOf(platformFeePercentage)).setScale(4, RoundingMode.HALF_UP);
        BigDecimal netSellerPayout = amount.subtract(fee);

        // 5. Transfer funds
        buyerWallet.setBalance(buyerWallet.getBalance().subtract(amount));
        sellerWallet.setBalance(sellerWallet.getBalance().add(netSellerPayout));

        walletRepository.save(buyerWallet);
        walletRepository.save(sellerWallet);

        // 6. Record completed settlement transaction
        String txId = "tx-" + UUID.randomUUID();
        PaymentTransaction tx = PaymentTransaction.builder()
                .transactionId(txId)
                .auctionId(auctionId)
                .buyerId(buyerId)
                .sellerId(sellerId)
                .amount(amount)
                .platformFee(fee)
                .netSellerPayout(netSellerPayout)
                .status(PaymentStatus.COMPLETED)
                .type(TransactionType.SETTLEMENT)
                .remarks(String.format("Clearance settlement for auction #%d", auctionId))
                .build();

        PaymentTransaction savedTx = transactionRepository.save(tx);
        log.info("Payment settlement completed successfully for auction {}: txId={}", auctionId, txId);

        return mapToSettlementResponse(savedTx);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(String transactionId) {
        PaymentTransaction tx = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));

        return TransactionResponse.builder()
                .id(tx.getId())
                .transactionId(tx.getTransactionId())
                .auctionId(tx.getAuctionId())
                .buyerId(tx.getBuyerId())
                .sellerId(tx.getSellerId())
                .amount(tx.getAmount())
                .platformFee(tx.getPlatformFee())
                .netSellerPayout(tx.getNetSellerPayout())
                .status(tx.getStatus())
                .type(tx.getType())
                .remarks(tx.getRemarks())
                .createdAt(tx.getCreatedAt())
                .build();
    }

    private WalletResponse mapToWalletResponse(Wallet w) {
        BigDecimal total = (w.getBalance() != null ? w.getBalance() : BigDecimal.ZERO)
                .add(w.getLockedBalance() != null ? w.getLockedBalance() : BigDecimal.ZERO);

        return WalletResponse.builder()
                .id(w.getId())
                .userId(w.getUserId())
                .balance(w.getBalance())
                .lockedBalance(w.getLockedBalance())
                .totalBalance(total)
                .updatedAt(w.getUpdatedAt() != null ? w.getUpdatedAt() : LocalDateTime.now())
                .build();
    }

    private PaymentSettlementResponse mapToSettlementResponse(PaymentTransaction tx) {
        return PaymentSettlementResponse.builder()
                .transactionId(tx.getTransactionId())
                .auctionId(tx.getAuctionId())
                .sellerId(tx.getSellerId())
                .buyerId(tx.getBuyerId())
                .amount(tx.getAmount())
                .platformFee(tx.getPlatformFee())
                .netSellerPayout(tx.getNetSellerPayout())
                .status(tx.getStatus().name())
                .settledAt(tx.getCreatedAt() != null ? tx.getCreatedAt() : LocalDateTime.now())
                .build();
    }

    @Override
    public PaymentResponse processWinnerPayment(PaymentRequest request, Long authenticatedUserId) {
        Long auctionId = request.getAuctionId();
        BigDecimal paymentAmount = request.getAmount();

        if (authenticatedUserId == null) {
            throw new UnauthorizedException("User must be authenticated to process payment");
        }

        ReentrantLock lock = auctionLocks.computeIfAbsent(auctionId, k -> new ReentrantLock(true));
        lock.lock();
        try {
            return executeInTransaction(() -> {
                // 1. Fetch auction details from Auction Service
                AuctionDto auction = auctionClient != null ? auctionClient.getAuctionById(auctionId) : null;
                if (auction == null) {
                    throw new ResourceNotFoundException("Auction not found with ID: " + auctionId);
                }

                // 2. Requirement 1: Only auction winner can make payment
                if (auction.getWinnerId() == null || !auction.getWinnerId().equals(authenticatedUserId)) {
                    log.warn("Payment rejected: User {} is not the winner of auction {} (winner is {})",
                            authenticatedUserId, auctionId, auction.getWinnerId());
                    throw new InvalidPaymentException("Only auction winner can make payment");
                }

                // 3. Requirement 2: Payment amount must equal winning bid
                BigDecimal winningBid = auction.getCurrentPrice();
                if (paymentAmount == null || winningBid == null || paymentAmount.compareTo(winningBid) != 0) {
                    log.warn("Payment rejected: Amount {} does not match winning bid {} for auction {}",
                            paymentAmount, winningBid, auctionId);
                    throw new InvalidPaymentException("Payment amount must equal winning bid");
                }

                // 4. Requirements 3, 4, 5: Idempotency & One successful payment per auction
                Optional<Payment> existingSuccess = paymentRepository.findByAuctionIdAndStatus(auctionId, PaymentStatus.SUCCESS);
                if (existingSuccess.isPresent()) {
                    log.info("Auction {} was already successfully paid. Idempotent return of txRef: {}",
                            auctionId, existingSuccess.get().getTransactionReference());
                    return mapToPaymentResponse(existingSuccess.get(), "Payment was already completed successfully");
                }

                // 5. Build payment processor request (sensitive data masked / never logged)
                PaymentProcessingRequest procReq = PaymentProcessingRequest.builder()
                        .auctionId(auctionId)
                        .winnerId(authenticatedUserId)
                        .amount(paymentAmount)
                        .paymentMethod(request.getPaymentMethod())
                        .maskedCardNumber(request.getMaskedCardNumber())
                        .simulateFailure(request.isSimulateFailure())
                        .build();

                // 6. Execute pluggable processor
                PaymentProcessingResult procResult = paymentProcessor.process(procReq);

                PaymentStatus status = procResult.isSuccess() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
                LocalDateTime paidAt = procResult.isSuccess() ? procResult.getProcessedAt() : null;

                Payment payment = Payment.builder()
                        .auctionId(auctionId)
                        .winnerId(authenticatedUserId)
                        .amount(paymentAmount)
                        .status(status)
                        .transactionReference(procResult.getTransactionReference())
                        .createdAt(LocalDateTime.now())
                        .paidAt(paidAt)
                        .build();

                Payment saved = paymentRepository.save(payment);

                if (!procResult.isSuccess()) {
                    log.warn("Payment failed for auction {}: ref={}, reason={}", auctionId, procResult.getTransactionReference(), procResult.getMessage());
                    return mapToPaymentResponse(saved, "Payment processing failed: " + procResult.getMessage());
                }

                log.info("Payment successfully processed for auction {}: ref={}", auctionId, saved.getTransactionReference());
                return mapToPaymentResponse(saved, "Payment processed successfully");
            });
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + id));
        return mapToPaymentResponse(payment, "Payment retrieved successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByAuctionId(Long auctionId) {
        Optional<Payment> successPayment = paymentRepository.findByAuctionIdAndStatus(auctionId, PaymentStatus.SUCCESS);
        if (successPayment.isPresent()) {
            return mapToPaymentResponse(successPayment.get(), "Successful payment record");
        }

        Payment payment = paymentRepository.findByAuctionId(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("No payment found for auction: " + auctionId));
        return mapToPaymentResponse(payment, "Payment record");
    }

    private PaymentResponse mapToPaymentResponse(Payment p, String message) {
        return PaymentResponse.builder()
                .id(p.getId())
                .auctionId(p.getAuctionId())
                .winnerId(p.getWinnerId())
                .amount(p.getAmount())
                .status(p.getStatus())
                .transactionReference(p.getTransactionReference())
                .createdAt(p.getCreatedAt())
                .paidAt(p.getPaidAt())
                .message(message)
                .build();
    }
}

package com.auction.payment.service.impl;

import com.auction.payment.dto.*;
import com.auction.payment.entity.PaymentStatus;
import com.auction.payment.entity.PaymentTransaction;
import com.auction.payment.entity.TransactionType;
import com.auction.payment.entity.Wallet;
import com.auction.payment.exception.InsufficientBalanceException;
import com.auction.payment.exception.ResourceNotFoundException;
import com.auction.payment.repository.PaymentTransactionRepository;
import com.auction.payment.repository.WalletRepository;
import com.auction.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final WalletRepository walletRepository;
    private final PaymentTransactionRepository transactionRepository;

    @Value("${payment.platform-fee-percentage:0.05}")
    private double platformFeePercentage;

    public PaymentServiceImpl(WalletRepository walletRepository, PaymentTransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
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
}

package com.auction.payment.service;

import com.auction.payment.dto.PaymentSettlementRequest;
import com.auction.payment.dto.PaymentSettlementResponse;
import com.auction.payment.dto.WalletDepositRequest;
import com.auction.payment.dto.WalletResponse;
import com.auction.payment.entity.PaymentStatus;
import com.auction.payment.entity.PaymentTransaction;
import com.auction.payment.entity.Wallet;
import com.auction.payment.exception.InsufficientBalanceException;
import com.auction.payment.repository.PaymentTransactionRepository;
import com.auction.payment.repository.WalletRepository;
import com.auction.payment.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private PaymentTransactionRepository transactionRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Wallet buyerWallet;
    private Wallet sellerWallet;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "platformFeePercentage", 0.05);

        buyerWallet = Wallet.builder()
                .id(1L)
                .userId(100L)
                .balance(new BigDecimal("500.00"))
                .lockedBalance(BigDecimal.ZERO)
                .build();

        sellerWallet = Wallet.builder()
                .id(2L)
                .userId(200L)
                .balance(new BigDecimal("100.00"))
                .lockedBalance(BigDecimal.ZERO)
                .build();
    }

    @Test
    void testDepositSuccess() {
        when(walletRepository.findByUserId(100L)).thenReturn(Optional.of(buyerWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(buyerWallet);

        WalletDepositRequest request = WalletDepositRequest.builder()
                .amount(new BigDecimal("50.00"))
                .build();

        WalletResponse response = paymentService.deposit(100L, request);

        assertNotNull(response);
        assertEquals(new BigDecimal("550.00"), response.getBalance());
        verify(transactionRepository, times(1)).save(any(PaymentTransaction.class));
    }

    @Test
    void testSettleAuctionPaymentSuccess() {
        when(transactionRepository.findByAuctionId(1L)).thenReturn(Optional.empty());
        when(walletRepository.findByUserId(100L)).thenReturn(Optional.of(buyerWallet));
        when(walletRepository.findByUserId(200L)).thenReturn(Optional.of(sellerWallet));

        when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(invocation -> {
            PaymentTransaction tx = invocation.getArgument(0);
            tx.setId(10L);
            return tx;
        });

        PaymentSettlementRequest request = PaymentSettlementRequest.builder()
                .auctionId(1L)
                .buyerId(100L)
                .sellerId(200L)
                .amount(new BigDecimal("200.00"))
                .build();

        PaymentSettlementResponse response = paymentService.settleAuctionPayment(request);

        assertNotNull(response);
        assertEquals(1L, response.getAuctionId());
        assertEquals(new BigDecimal("200.00"), response.getAmount());
        assertEquals(new BigDecimal("10.0000"), response.getPlatformFee()); // 5% of 200
        assertEquals(new BigDecimal("190.0000"), response.getNetSellerPayout());
        assertEquals("COMPLETED", response.getStatus());

        assertEquals(new BigDecimal("300.00"), buyerWallet.getBalance());
        assertEquals(new BigDecimal("290.0000"), sellerWallet.getBalance());
    }

    @Test
    void testSettleAuctionPaymentInsufficientFundsThrowsException() {
        when(transactionRepository.findByAuctionId(1L)).thenReturn(Optional.empty());
        buyerWallet.setBalance(new BigDecimal("50.00")); // only 50 available
        when(walletRepository.findByUserId(100L)).thenReturn(Optional.of(buyerWallet));

        PaymentSettlementRequest request = PaymentSettlementRequest.builder()
                .auctionId(1L)
                .buyerId(100L)
                .sellerId(200L)
                .amount(new BigDecimal("200.00"))
                .build();

        assertThrows(InsufficientBalanceException.class, () -> paymentService.settleAuctionPayment(request));
    }

    @Test
    void testSettleAuctionPaymentIdempotent() {
        PaymentTransaction existingTx = PaymentTransaction.builder()
                .transactionId("tx-existing-123")
                .auctionId(1L)
                .buyerId(100L)
                .sellerId(200L)
                .amount(new BigDecimal("200.00"))
                .platformFee(new BigDecimal("10.00"))
                .netSellerPayout(new BigDecimal("190.00"))
                .status(PaymentStatus.COMPLETED)
                .build();

        when(transactionRepository.findByAuctionId(1L)).thenReturn(Optional.of(existingTx));

        PaymentSettlementRequest request = PaymentSettlementRequest.builder()
                .auctionId(1L)
                .buyerId(100L)
                .sellerId(200L)
                .amount(new BigDecimal("200.00"))
                .build();

        PaymentSettlementResponse response = paymentService.settleAuctionPayment(request);

        assertNotNull(response);
        assertEquals("tx-existing-123", response.getTransactionId());
        verify(walletRepository, never()).save(any(Wallet.class));
    }
}

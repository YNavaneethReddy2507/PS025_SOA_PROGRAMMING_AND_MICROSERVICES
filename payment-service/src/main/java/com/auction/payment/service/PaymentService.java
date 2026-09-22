package com.auction.payment.service;

import com.auction.payment.dto.*;

public interface PaymentService {
    WalletResponse deposit(Long userId, WalletDepositRequest request);
    WalletResponse getWalletByUserId(Long userId);
    PaymentSettlementResponse settleAuctionPayment(PaymentSettlementRequest request);
    TransactionResponse getTransactionById(String transactionId);
    PaymentResponse processWinnerPayment(PaymentRequest request, Long authenticatedUserId);
    PaymentResponse getPaymentById(Long id);
    PaymentResponse getPaymentById(Long id, Long callerUserId, String callerRole);
    PaymentResponse getPaymentByAuctionId(Long auctionId);
    PaymentResponse getPaymentByAuctionId(Long auctionId, Long callerUserId, String callerRole);
    WalletResponse getWalletByUserId(Long userId, Long callerUserId, String callerRole);
}

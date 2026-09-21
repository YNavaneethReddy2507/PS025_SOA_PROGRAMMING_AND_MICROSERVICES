package com.auction.payment.service;

import com.auction.payment.dto.*;

public interface PaymentService {
    WalletResponse deposit(Long userId, WalletDepositRequest request);
    WalletResponse getWalletByUserId(Long userId);
    PaymentSettlementResponse settleAuctionPayment(PaymentSettlementRequest request);
    TransactionResponse getTransactionById(String transactionId);
    PaymentResponse processWinnerPayment(PaymentRequest request, Long authenticatedUserId);
    PaymentResponse getPaymentById(Long id);
    PaymentResponse getPaymentByAuctionId(Long auctionId);
}

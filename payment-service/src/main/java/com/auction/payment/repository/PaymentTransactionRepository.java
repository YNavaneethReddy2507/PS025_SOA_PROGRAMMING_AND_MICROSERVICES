package com.auction.payment.repository;

import com.auction.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByTransactionId(String transactionId);
    Optional<PaymentTransaction> findByAuctionId(Long auctionId);
    List<PaymentTransaction> findByBuyerId(Long buyerId);
    List<PaymentTransaction> findBySellerId(Long sellerId);
}

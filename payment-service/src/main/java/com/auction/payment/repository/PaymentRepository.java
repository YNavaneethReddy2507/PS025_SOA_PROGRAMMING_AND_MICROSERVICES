package com.auction.payment.repository;

import com.auction.payment.entity.Payment;
import com.auction.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByAuctionId(Long auctionId);

    List<Payment> findAllByAuctionId(Long auctionId);

    Optional<Payment> findByAuctionIdAndStatus(Long auctionId, PaymentStatus status);

    Optional<Payment> findByTransactionReference(String transactionReference);

    List<Payment> findByWinnerId(Long winnerId);

    boolean existsByAuctionIdAndStatus(Long auctionId, PaymentStatus status);
}

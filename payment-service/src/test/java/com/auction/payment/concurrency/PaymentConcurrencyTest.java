package com.auction.payment.concurrency;

import com.auction.payment.client.AuctionClient;
import com.auction.payment.dto.AuctionDto;
import com.auction.payment.dto.PaymentRequest;
import com.auction.payment.dto.PaymentResponse;
import com.auction.payment.entity.Payment;
import com.auction.payment.entity.PaymentStatus;
import com.auction.payment.repository.PaymentRepository;
import com.auction.payment.repository.PaymentTransactionRepository;
import com.auction.payment.repository.WalletRepository;
import com.auction.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class PaymentConcurrencyTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentTransactionRepository transactionRepository;

    @Autowired
    private WalletRepository walletRepository;

    @MockBean
    private AuctionClient auctionClient;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
    }

    @Test
    @DisplayName("Concurrency: Simultaneous duplicate payments - exactly one payment executed, idempotent result")
    void testConcurrentPaymentIdempotency() throws InterruptedException {
        Long auctionId = 99L;
        Long winnerId = 100L;
        BigDecimal amount = new BigDecimal("500.00");

        AuctionDto auction = AuctionDto.builder()
                .id(auctionId)
                .title("Rare Artwork")
                .startingPrice(new BigDecimal("100.00"))
                .currentPrice(amount)
                .winnerId(winnerId)
                .sellerId(10L)
                .status("CLOSED")
                .build();

        when(auctionClient.getAuctionById(auctionId)).thenReturn(auction);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        Set<String> transactionReferences = Collections.newSetFromMap(new ConcurrentHashMap<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    PaymentRequest request = PaymentRequest.builder()
                            .auctionId(auctionId)
                            .winnerId(winnerId)
                            .amount(amount)
                            .paymentMethod("CREDIT_CARD")
                            .build();

                    PaymentResponse response = paymentService.processWinnerPayment(request, winnerId);
                    if (response != null && response.getStatus() == PaymentStatus.SUCCESS) {
                        successCount.incrementAndGet();
                        transactionReferences.add(response.getTransactionReference());
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Trigger all 10 threads concurrently
        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "Timed out waiting for concurrent payments");
        executor.shutdown();

        // All concurrent calls should complete successfully due to idempotent handling
        assertEquals(threadCount, successCount.get(), "All concurrent requests should return SUCCESS status");

        // Exactly one unique transaction reference across all returned responses
        assertEquals(1, transactionReferences.size(), "All threads must receive the EXACT same transaction reference");

        // Exactly one payment record in the database
        List<Payment> persistedPayments = paymentRepository.findAll();
        assertEquals(1, persistedPayments.size(), "Database must contain exactly ONE payment record for the auction");

        Payment payment = persistedPayments.get(0);
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals(winnerId, payment.getWinnerId());
        assertEquals(0, amount.compareTo(payment.getAmount()));
        assertEquals(transactionReferences.iterator().next(), payment.getTransactionReference());
    }
}

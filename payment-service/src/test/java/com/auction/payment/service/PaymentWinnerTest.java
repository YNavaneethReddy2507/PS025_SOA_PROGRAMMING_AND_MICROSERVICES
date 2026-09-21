package com.auction.payment.service;

import com.auction.payment.client.AuctionClient;
import com.auction.payment.dto.AuctionDto;
import com.auction.payment.dto.PaymentRequest;
import com.auction.payment.dto.PaymentResponse;
import com.auction.payment.entity.Payment;
import com.auction.payment.entity.PaymentStatus;
import com.auction.payment.exception.InvalidPaymentException;
import com.auction.payment.exception.ResourceNotFoundException;
import com.auction.payment.processor.MockPaymentProcessor;
import com.auction.payment.processor.PaymentProcessor;
import com.auction.payment.repository.PaymentRepository;
import com.auction.payment.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentWinnerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AuctionClient auctionClient;

    @Spy
    private PaymentProcessor paymentProcessor = new MockPaymentProcessor();

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private AuctionDto sampleAuction;

    @BeforeEach
    void setUp() {
        sampleAuction = AuctionDto.builder()
                .id(1L)
                .title("Rolex Submariner")
                .startingPrice(new BigDecimal("100.00"))
                .currentPrice(new BigDecimal("250.00"))
                .winnerId(100L)
                .sellerId(50L)
                .status("CLOSED")
                .build();
    }

    private void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, expected.compareTo(actual));
    }

    @Test
    @DisplayName("Successful payment by auction winner with exact winning amount")
    void testSuccessfulPayment() {
        when(auctionClient.getAuctionById(1L)).thenReturn(sampleAuction);
        when(paymentRepository.findByAuctionIdAndStatus(1L, PaymentStatus.SUCCESS)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(10L);
            return p;
        });

        PaymentRequest request = PaymentRequest.builder()
                .auctionId(1L)
                .winnerId(100L)
                .amount(new BigDecimal("250.00"))
                .paymentMethod("CREDIT_CARD")
                .maskedCardNumber("**** 1234")
                .build();

        PaymentResponse response = paymentService.processWinnerPayment(request, 100L);

        assertNotNull(response);
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals(100L, response.getWinnerId());
        assertBigDecimalEquals(new BigDecimal("250.00"), response.getAmount());
        assertTrue(response.getTransactionReference().startsWith("PAY-"));
        assertNotNull(response.getPaidAt());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Payment rejected when authenticated user is not the auction winner")
    void testInvalidWinner() {
        when(auctionClient.getAuctionById(1L)).thenReturn(sampleAuction);

        PaymentRequest request = PaymentRequest.builder()
                .auctionId(1L)
                .winnerId(999L)
                .amount(new BigDecimal("250.00"))
                .build();

        InvalidPaymentException ex = assertThrows(InvalidPaymentException.class, () ->
                paymentService.processWinnerPayment(request, 999L)
        );

        assertTrue(ex.getMessage().contains("Only auction winner can make payment"));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Payment rejected when payment amount differs from winning bid")
    void testWrongAmount() {
        when(auctionClient.getAuctionById(1L)).thenReturn(sampleAuction);

        PaymentRequest request = PaymentRequest.builder()
                .auctionId(1L)
                .winnerId(100L)
                .amount(new BigDecimal("200.00")) // Expected is 250.00
                .build();

        InvalidPaymentException ex = assertThrows(InvalidPaymentException.class, () ->
                paymentService.processWinnerPayment(request, 100L)
        );

        assertTrue(ex.getMessage().contains("Payment amount must equal winning bid"));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Idempotency: Duplicate payment returns existing successful payment without duplicate charge")
    void testDuplicatePayment() {
        when(auctionClient.getAuctionById(1L)).thenReturn(sampleAuction);

        Payment existingPayment = Payment.builder()
                .id(10L)
                .auctionId(1L)
                .winnerId(100L)
                .amount(new BigDecimal("250.00"))
                .status(PaymentStatus.SUCCESS)
                .transactionReference("PAY-ORIGINAL-REF-12345")
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .paidAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(paymentRepository.findByAuctionIdAndStatus(1L, PaymentStatus.SUCCESS))
                .thenReturn(Optional.of(existingPayment));

        PaymentRequest request = PaymentRequest.builder()
                .auctionId(1L)
                .winnerId(100L)
                .amount(new BigDecimal("250.00"))
                .build();

        PaymentResponse response = paymentService.processWinnerPayment(request, 100L);

        assertNotNull(response);
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals("PAY-ORIGINAL-REF-12345", response.getTransactionReference());
        assertEquals("Payment was already completed successfully", response.getMessage());
        // Verify no second payment record was saved
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Payment failure handled gracefully and recorded with FAILED status")
    void testPaymentFailure() {
        when(auctionClient.getAuctionById(1L)).thenReturn(sampleAuction);
        when(paymentRepository.findByAuctionIdAndStatus(1L, PaymentStatus.SUCCESS)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(20L);
            return p;
        });

        PaymentRequest request = PaymentRequest.builder()
                .auctionId(1L)
                .winnerId(100L)
                .amount(new BigDecimal("250.00"))
                .simulateFailure(true)
                .build();

        PaymentResponse response = paymentService.processWinnerPayment(request, 100L);

        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertNull(response.getPaidAt());
        assertTrue(response.getMessage().contains("failed") || response.getMessage().contains("declined"));
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Retry: Failed payment allows subsequent retry that succeeds")
    void testRetry() {
        when(auctionClient.getAuctionById(1L)).thenReturn(sampleAuction);
        // Initially no successful payment exists
        when(paymentRepository.findByAuctionIdAndStatus(1L, PaymentStatus.SUCCESS)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(30L);
            return p;
        });

        // 1. First attempt fails
        PaymentRequest failedRequest = PaymentRequest.builder()
                .auctionId(1L)
                .winnerId(100L)
                .amount(new BigDecimal("250.00"))
                .simulateFailure(true)
                .build();

        PaymentResponse firstResponse = paymentService.processWinnerPayment(failedRequest, 100L);
        assertEquals(PaymentStatus.FAILED, firstResponse.getStatus());

        // 2. Second retry attempt succeeds
        PaymentRequest retryRequest = PaymentRequest.builder()
                .auctionId(1L)
                .winnerId(100L)
                .amount(new BigDecimal("250.00"))
                .simulateFailure(false)
                .build();

        PaymentResponse retryResponse = paymentService.processWinnerPayment(retryRequest, 100L);
        assertEquals(PaymentStatus.SUCCESS, retryResponse.getStatus());
        assertNotNull(retryResponse.getPaidAt());
        assertTrue(retryResponse.getTransactionReference().startsWith("PAY-"));
        verify(paymentRepository, times(2)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Idempotency: Multiple calls return identical transaction reference")
    void testIdempotency() {
        when(auctionClient.getAuctionById(1L)).thenReturn(sampleAuction);
        Payment existingPayment = Payment.builder()
                .id(99L)
                .auctionId(1L)
                .winnerId(100L)
                .amount(new BigDecimal("250.00"))
                .status(PaymentStatus.SUCCESS)
                .transactionReference("PAY-IDEMPOTENT-XYZ")
                .createdAt(LocalDateTime.now())
                .paidAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findByAuctionIdAndStatus(1L, PaymentStatus.SUCCESS))
                .thenReturn(Optional.of(existingPayment));

        PaymentRequest request = PaymentRequest.builder()
                .auctionId(1L)
                .winnerId(100L)
                .amount(new BigDecimal("250.00"))
                .build();

        PaymentResponse r1 = paymentService.processWinnerPayment(request, 100L);
        PaymentResponse r2 = paymentService.processWinnerPayment(request, 100L);

        assertEquals(r1.getTransactionReference(), r2.getTransactionReference());
        assertEquals(PaymentStatus.SUCCESS, r1.getStatus());
        assertEquals(PaymentStatus.SUCCESS, r2.getStatus());
    }
}

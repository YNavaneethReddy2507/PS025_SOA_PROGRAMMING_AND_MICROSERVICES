package com.auction.payment.security;

import com.auction.payment.client.AuctionClient;
import com.auction.payment.dto.AuctionDto;
import com.auction.payment.dto.PaymentRequest;
import com.auction.payment.dto.PaymentResponse;
import com.auction.payment.dto.WalletResponse;
import com.auction.payment.entity.Payment;
import com.auction.payment.entity.PaymentStatus;
import com.auction.payment.entity.Wallet;
import com.auction.payment.exception.AccessDeniedException;
import com.auction.payment.exception.UnauthorizedException;
import com.auction.payment.repository.PaymentRepository;
import com.auction.payment.repository.PaymentTransactionRepository;
import com.auction.payment.repository.WalletRepository;
import com.auction.payment.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentSecurityTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentTransactionRepository transactionRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private AuctionClient auctionClient;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Payment samplePayment;
    private Wallet sampleWallet;

    @BeforeEach
    void setUp() {
        samplePayment = Payment.builder()
                .id(100L)
                .auctionId(10L)
                .winnerId(42L)
                .amount(new BigDecimal("250.00"))
                .status(PaymentStatus.SUCCESS)
                .transactionReference("TX-REF-100")
                .createdAt(LocalDateTime.now())
                .paidAt(LocalDateTime.now())
                .build();

        sampleWallet = Wallet.builder()
                .id(1L)
                .userId(42L)
                .balance(new BigDecimal("1000.00"))
                .lockedBalance(BigDecimal.ZERO)
                .build();
    }

    @Test
    @DisplayName("Winner can access their own payment details")
    void testWinnerCanAccessPayment() {
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(samplePayment));

        PaymentResponse response = paymentService.getPaymentById(100L, 42L, "ROLE_USER");
        assertNotNull(response);
        assertEquals(42L, response.getWinnerId());
    }

    @Test
    @DisplayName("Seller can access payment details for their auction")
    void testSellerCanAccessPayment() {
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(samplePayment));
        AuctionDto auction = AuctionDto.builder().id(10L).sellerId(88L).build();
        when(auctionClient.getAuctionById(10L)).thenReturn(auction);

        PaymentResponse response = paymentService.getPaymentById(100L, 88L, "ROLE_USER");
        assertNotNull(response);
        assertEquals(100L, response.getId());
    }

    @Test
    @DisplayName("Admin can access any payment details")
    void testAdminCanAccessAnyPayment() {
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(samplePayment));

        PaymentResponse response = paymentService.getPaymentById(100L, 999L, "ROLE_ADMIN");
        assertNotNull(response);
        assertEquals(100L, response.getId());
    }

    @Test
    @DisplayName("Unauthorized third party is forbidden from accessing payment details")
    void testUnauthorizedUserDeniedPaymentAccess() {
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(samplePayment));
        AuctionDto auction = AuctionDto.builder().id(10L).sellerId(88L).build();
        when(auctionClient.getAuctionById(10L)).thenReturn(auction);

        assertThrows(AccessDeniedException.class, () -> paymentService.getPaymentById(100L, 777L, "ROLE_USER"));
    }

    @Test
    @DisplayName("Unauthenticated caller (null callerUserId) is denied access")
    void testUnauthenticatedUserDeniedPaymentAccess() {
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(samplePayment));

        assertThrows(UnauthorizedException.class, () -> paymentService.getPaymentById(100L, null, "ROLE_USER"));
    }

    @Test
    @DisplayName("User can view their own wallet")
    void testUserCanViewOwnWallet() {
        when(walletRepository.findByUserId(42L)).thenReturn(Optional.of(sampleWallet));

        WalletResponse response = paymentService.getWalletByUserId(42L, 42L, "ROLE_USER");
        assertNotNull(response);
        assertEquals(new BigDecimal("1000.00"), response.getBalance());
    }

    @Test
    @DisplayName("User cannot view another user's wallet")
    void testUserCannotViewOtherWallet() {
        assertThrows(AccessDeniedException.class, () -> paymentService.getWalletByUserId(42L, 99L, "ROLE_USER"));
    }

    @Test
    @DisplayName("Admin can view any user's wallet")
    void testAdminCanViewAnyWallet() {
        when(walletRepository.findByUserId(42L)).thenReturn(Optional.of(sampleWallet));

        WalletResponse response = paymentService.getWalletByUserId(42L, 999L, "ROLE_ADMIN");
        assertNotNull(response);
        assertEquals(new BigDecimal("1000.00"), response.getBalance());
    }

    @Test
    @DisplayName("Payment processing without authentication throws UnauthorizedException")
    void testPaymentWithoutAuthThrowsUnauthorized() {
        PaymentRequest request = PaymentRequest.builder()
                .auctionId(10L)
                .amount(new BigDecimal("250.00"))
                .build();

        assertThrows(UnauthorizedException.class, () -> paymentService.processWinnerPayment(request, null));
    }
}

package com.auction.payment.controller;

import com.auction.payment.dto.PaymentSettlementRequest;
import com.auction.payment.dto.PaymentSettlementResponse;
import com.auction.payment.dto.WalletDepositRequest;
import com.auction.payment.dto.WalletResponse;
import com.auction.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @Test
    void testDepositEndpoint() throws Exception {
        WalletDepositRequest request = WalletDepositRequest.builder()
                .amount(new BigDecimal("100.00"))
                .build();

        WalletResponse response = WalletResponse.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("100.00"))
                .lockedBalance(BigDecimal.ZERO)
                .totalBalance(new BigDecimal("100.00"))
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentService.deposit(eq(1L), any(WalletDepositRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/wallet/deposit")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.balance").value(100.00));
    }

    @Test
    void testGetWalletEndpoint() throws Exception {
        WalletResponse response = WalletResponse.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("250.00"))
                .lockedBalance(BigDecimal.ZERO)
                .totalBalance(new BigDecimal("250.00"))
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentService.getWalletByUserId(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/payments/wallet")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(250.00));
    }

    @Test
    void testSettlePaymentEndpoint() throws Exception {
        PaymentSettlementRequest request = PaymentSettlementRequest.builder()
                .auctionId(1L)
                .buyerId(10L)
                .sellerId(20L)
                .amount(new BigDecimal("500.00"))
                .build();

        PaymentSettlementResponse response = PaymentSettlementResponse.builder()
                .transactionId("tx-123")
                .auctionId(1L)
                .buyerId(10L)
                .sellerId(20L)
                .amount(new BigDecimal("500.00"))
                .platformFee(new BigDecimal("25.00"))
                .netSellerPayout(new BigDecimal("475.00"))
                .status("COMPLETED")
                .settledAt(LocalDateTime.now())
                .build();

        when(paymentService.settleAuctionPayment(any(PaymentSettlementRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/settle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value("tx-123"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void testProcessWinnerPaymentEndpoint() throws Exception {
        com.auction.payment.dto.PaymentRequest request = com.auction.payment.dto.PaymentRequest.builder()
                .auctionId(1L)
                .winnerId(10L)
                .amount(new BigDecimal("500.00"))
                .paymentMethod("CREDIT_CARD")
                .build();

        com.auction.payment.dto.PaymentResponse response = com.auction.payment.dto.PaymentResponse.builder()
                .id(100L)
                .auctionId(1L)
                .winnerId(10L)
                .amount(new BigDecimal("500.00"))
                .status(com.auction.payment.entity.PaymentStatus.SUCCESS)
                .transactionReference("PAY-REF-123")
                .paidAt(LocalDateTime.now())
                .build();

        when(paymentService.processWinnerPayment(any(com.auction.payment.dto.PaymentRequest.class), eq(10L)))
                .thenReturn(response);

        mockMvc.perform(post("/api/payments")
                        .header("X-User-Id", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.transactionReference").value("PAY-REF-123"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void testGetPaymentByIdEndpoint() throws Exception {
        com.auction.payment.dto.PaymentResponse response = com.auction.payment.dto.PaymentResponse.builder()
                .id(100L)
                .auctionId(1L)
                .winnerId(10L)
                .amount(new BigDecimal("500.00"))
                .status(com.auction.payment.entity.PaymentStatus.SUCCESS)
                .transactionReference("PAY-REF-123")
                .paidAt(LocalDateTime.now())
                .build();

        when(paymentService.getPaymentById(100L)).thenReturn(response);

        mockMvc.perform(get("/api/payments/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.transactionReference").value("PAY-REF-123"));
    }

    @Test
    void testGetPaymentByAuctionIdEndpoint() throws Exception {
        com.auction.payment.dto.PaymentResponse response = com.auction.payment.dto.PaymentResponse.builder()
                .id(100L)
                .auctionId(1L)
                .winnerId(10L)
                .amount(new BigDecimal("500.00"))
                .status(com.auction.payment.entity.PaymentStatus.SUCCESS)
                .transactionReference("PAY-REF-123")
                .paidAt(LocalDateTime.now())
                .build();

        when(paymentService.getPaymentByAuctionId(1L)).thenReturn(response);

        mockMvc.perform(get("/api/payments/auction/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auctionId").value(1L))
                .andExpect(jsonPath("$.transactionReference").value("PAY-REF-123"));
    }
}
